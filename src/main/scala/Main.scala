import java.lang.Object
import java.util
import java.util.UUID
import java.util.concurrent.{TimeoutException, TimeUnit}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import auth.TokenAuthenticator
import com.mongodb.DBCursor
import com.stormpath.sdk.directory.CustomData
import db.{UserNotPresent, UserAlreadyAdded, EventNotFound}
import model._
import service.{GetUserById, GetUserByToken, UserService}
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes
import spray.http.StatusCodes._
import spray.routing.{AuthenticationFailedRejection, _}
import spray.util.LoggingContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object Main extends App with SimpleRoutingApp {

    implicit val system = ActorSystem("my-system")
    implicit val dbService = new db.DBService()
    implicit val userService = system.actorOf(Props[UserService], "user-service")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    val authenticator = TokenAuthenticator[User](
        headerName = "token",
        queryStringParameterName = "token"
    ) { key =>
        (userService ? GetUserByToken(key)).mapTo[Option[User]].map(result => result)
    }

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {

        import format.EventJsonFormat._
        import format.UserDataJsonFormat._
        import spray.httpx.SprayJsonSupport._
        import format.APIResponseFormat._

        path("") {
            get {
                complete("OK")
            }
        } ~
            handleRejections(MyRejectionHandler.jsonRejectionHandler) {
                handleExceptions(MyExceptionHandler.myExceptionHandler) {
                    authenticate(authenticator) { user =>
                        path("event") {
                            post {
                                entity(as[Event]) {
                                    event =>
                                        complete{
                                            dbService.saveEvent(user, event)
                                            APIResponse("ok")
                                        }
                                }
                            } ~ get {
                                parameters('x.as[Double], 'y.as[Double], 'max.as[Long], 'tags.as[String]?) {
                                    (x, y, max, t) =>
                                        complete {
                                            val tags: Array[String] = if (t.nonEmpty) t.get.split(",+") else Array.empty[String]
                                            val events: DBCursor = dbService.findEvents(x, y, max, tags)
                                            dbService.toJson(events)
                                        }
                                }
                            }
                        } ~
                        pathPrefix("event" / Segment / "user") { id =>
                            pathEnd {
                                put {
                                    complete {
                                        dbService.toJson(dbService.addParticipant(id, user))
                                        //APIResponse("ok")
                                    }
                                } ~
                                delete {
                                    complete {
                                        dbService.toJson(dbService.removeParticipant(id, user))
                                        //APIResponse("ok")
                                    }
                                }
                            }
                        } ~
                        pathPrefix("event" / Segment / "comment" / Segment) { (id, msg) =>
                            pathEnd {
                                put {
                                    complete {
                                        dbService.toJson(dbService.addComment(id, user, msg))
                                    }
                                }
                            }
                        } ~
                        path("user") {
                            post {
                                entity(as[UserData]) {
                                    userData =>
                                        respondWithHeader(RawHeader("Location", user.id)) {
                                            complete {
                                                val customData: CustomData = user.account.getCustomData
                                                customData.put("photo_url", userData.photo_url)
                                                customData.put("age", userData.age.toString)
                                                customData.put("bio", userData.bio.toString)
                                                customData.put("tags", userData.tags)
                                                customData.put("first_name", userData.firstName)
                                                customData.put("last_name", userData.lastName)
                                                user.account.save()
                                                APIResponse("OK")
                                            }
                                        }
                                }
                            }
                        } ~
                        pathPrefix("user" / Segment) { id =>
                            pathEnd {
                                get {
                                    complete{
                                        (userService ? GetUserById(id)).mapTo[Option[User]].map(result => {
                                            val customData : CustomData = result.get.account.getCustomData
                                            val photo_url: String = customData.get("photo_url").toString
                                            val age: Int = customData.get("age").toString.toInt
                                            val bio: String = customData.get("bio").toString
                                            val tags: util.ArrayList[String] = customData.get("tags").asInstanceOf[util.ArrayList[String]]
                                            val firstName : String = customData.get("first_name").toString
                                            val lastName : String = customData.get("last_name").toString
                                            UserData(photo_url, age, bio, tags.toArray(new Array[String](tags.size())), firstName, lastName)}
                                        )
                                    }
                                }
                            } ~
                            path("events") {
                                get {
                                    complete {
                                        dbService.toJson(dbService.findEvents(id))
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    object MyRejectionHandler {

        import format.ErrorJsonFormat._
        import spray.httpx.SprayJsonSupport._

        implicit val jsonRejectionHandler = RejectionHandler {
            case AuthenticationFailedRejection(msg, cause) :: Nil =>
                complete(StatusCodes.Unauthorized, APIError("Authentication failure"))
            case MissingQueryParamRejection(name) :: Nil =>
                complete(StatusCodes.BadRequest, APIError("Parameter missing: "+name))
            case MalformedRequestContentRejection(message, cause) :: Nil =>
                complete(StatusCodes.BadRequest, APIError(message))
            case _ :: Nil =>
                complete(StatusCodes.BadRequest, APIError("Failed to process request"))
        }
    }

    object MyExceptionHandler {

        import format.ErrorJsonFormat._
        import spray.httpx.SprayJsonSupport._

        implicit def myExceptionHandler(implicit log: LoggingContext) =

            ExceptionHandler {
                case e: TimeoutException =>
                    requestUri { uri =>
                        log.warning("Request to {} exceeded max time", uri)
                        val error: APIError = new APIError("Request timeout")
                        complete(RequestTimeout, error)
                    }
                case e: NoSuchElementException =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("Object not found")
                        complete(NotFound, error)
                    }
                case e: EventNotFound =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("Event not found")
                        complete(NotFound, error)
                    }
                case e: UserAlreadyAdded =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("User already added to this event")
                        complete(Conflict, error)
                    }
                case e: UserNotPresent =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("User not part of this event")
                        complete(NotFound, error)
                    }
                case e: Exception =>
                    requestUri { uri =>
                        log.error(e, "Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError("Internal server error")
                        complete(InternalServerError, error)
                    }
            }
    }

}
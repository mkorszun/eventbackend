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
import model._
import service.{GetUserById, GetUserByToken, UserService}
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
                                parameters('x.as[Double], 'y.as[Double], 'max.as[Long]) {
                                    (x, y, max) =>
                                        complete {
                                            val events: DBCursor = dbService.findEvents(x, y, max)
                                            dbService.toJson(events)
                                        }
                                }
                            }
                        } ~
                        pathPrefix("event" / Segment / "user") { id =>
                            pathEnd {
                                put {
                                    complete {
                                        dbService.addParticipant(id, user)
                                        APIResponse("ok")
                                    }
                                } ~
                                delete {
                                    complete {
                                        dbService.removeParticipant(id, user)
                                        APIResponse("ok")
                                    }
                                }
                            }
                        } ~
                        path("user") {
                            post {
                                entity(as[UserData]) {
                                    userData =>
                                        complete{
                                            val customData: CustomData = user.account.getCustomData
                                            customData.put("photo_url", userData.photo_url)
                                            customData.put("age", userData.age.toString)
                                            customData.put("bio", userData.bio.toString)
                                            customData.put("tags", userData.tags)
                                            user.account.save()
                                            APIResponse("ok")
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
                                            UserData(photo_url, age, bio, tags.toArray(new Array[String](tags.size())))}
                                        )
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
                        complete(RequestTimeout, error)
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
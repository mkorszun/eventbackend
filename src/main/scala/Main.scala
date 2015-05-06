import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import auth.TokenAuthenticator
import com.mongodb.DBCursor
import com.stormpath.sdk.directory.CustomData
import model.{UserData, APIError, Event, User}
import service.{GetUser, UserService}
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
        val future = userService ? GetUser(key)
        val user = Await.result(future, timeout.duration).asInstanceOf[Option[User]]
        Future(user)
    }

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {

        import format.EventJsonFormat._
        import format.UserDataJsonFormat._
        import spray.httpx.SprayJsonSupport._

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
                                        dbService.saveEvent(user, event)
                                        complete("OK")
                                }
                            } ~ get {
                                parameters('x.as[Double], 'y.as[Double], 'max.as[Long]) {
                                    (x, y, max) =>
                                        val events: DBCursor = dbService.findEvents(x, y, max)
                                        complete(dbService.toJson(events))
                                }
                            }
                        } ~
                        path("user") {
                            post {
                                entity(as[UserData]) {
                                    userData =>
                                        val customData: CustomData = user.account.getCustomData
                                        customData.put("photo_url", userData.photo_url)
                                        customData.put("age", userData.age.toString)
                                        user.account.save()
                                        complete("OK")
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
                complete(StatusCodes.BadRequest, APIError(msg.toString))
            case _ =>
                complete(StatusCodes.BadRequest, APIError("Failed to process request"))
        }
    }

    object MyExceptionHandler {

        import format.ErrorJsonFormat._
        import spray.httpx.SprayJsonSupport._

        implicit def myExceptionHandler(implicit log: LoggingContext) =

            ExceptionHandler {
                case e: Exception =>
                    requestUri { uri =>
                        log.warning("Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError(e.getMessage)
                        complete(InternalServerError, error)
                    }
            }
    }

}
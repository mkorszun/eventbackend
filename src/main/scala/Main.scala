import java.util._
import java.util.concurrent.{TimeUnit, TimeoutException}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import auth.TokenAuthenticator
import doc.Documentation
import model._
import routes.{EventRoute, UserRoute}
import service._
import spray.http.StatusCodes
import spray.http.StatusCodes._
import spray.routing.{AuthenticationFailedRejection, _}
import spray.util.LoggingContext

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App with SimpleRoutingApp {

    implicit val system = ActorSystem("my-system")
    implicit val eventService = new EventService()
    implicit val userService = system.actorOf(Props[UserService], "user-service")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    val authenticator = TokenAuthenticator[User](headerName = "token", queryStringParameterName = "token") { key =>
        (userService ? GetUserByToken(key)).mapTo[Option[User]].map(result => result)
    }

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {

        path("") {
            get {
                complete("OK")
            }
        } ~ Documentation.docRoutes() ~
            handleRejections(MyRejectionHandler.jsonRejectionHandler) {
                handleExceptions(MyExceptionHandler.myExceptionHandler) {
                    authenticate(authenticator) { user =>
                        EventRoute.route(user) ~ UserRoute.route(user)
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
                complete(StatusCodes.BadRequest, APIError("Parameter missing: " + name))
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
import java.util._
import java.util.concurrent.TimeoutException

import _root_.directives.UnauthorizedException
import akka.actor._
import auth.TokenAuthenticator
import doc.Documentation
import model._
import model.user.User
import service.http.{EventHTTPService, TagHTTPService, TokenHTTPService, UserHTTPService}
import service.storage.events.{EventNotFound, UserAlreadyAdded, UserNotPresent}
import service.storage.users.UserStorageService
import spray.http.StatusCodes
import spray.http.StatusCodes._
import spray.routing._
import spray.util.LoggingContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App with SimpleRoutingApp with CORSSupport {

    implicit val system = ActorSystem("my-system")

    val authenticator1 = TokenAuthenticator[User](
        headerName = "token",
        queryStringParameterName = "token") { key =>
        Future {
            UserStorageService.readPrivateUserData(key)
        }
    }

    val events_service = new EventHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = actorRefFactory
    }

    val user_service = new UserHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = actorRefFactory
    }

    val token_service = new TokenHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = actorRefFactory
    }

    val tag_service = new TagHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = actorRefFactory
    }

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {
        cors {
            path("") {
                get {
                    complete("OK")
                }
            } ~
                events_service.public_routes() ~
                tag_service.public_routes() ~
                token_service.routes() ~
                path("documentation") {
                    redirect(System.getenv("DOC_URL"), MovedPermanently)
                } ~
                Documentation.docRoutes() ~
                handleRejections(MyRejectionHandler.jsonRejectionHandler) {
                    handleExceptions(MyExceptionHandler.myExceptionHandler) {
                        authenticate(authenticator1) { user =>
                            events_service.routes(user) ~ user_service.routes(user)
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
                complete((StatusCodes.Unauthorized, APIError("Authentication failure")))
            case MissingQueryParamRejection(name) :: Nil =>
                complete((StatusCodes.BadRequest, APIError("Parameter missing: " + name)))
            case MalformedRequestContentRejection(message, cause) :: Nil =>
                complete((StatusCodes.BadRequest, APIError(message)))
            case _ :: Nil =>
                complete((StatusCodes.BadRequest, APIError("Failed to process request")))
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
                        complete((RequestTimeout, error))
                    }
                case e: UnauthorizedException =>
                    requestUri { uri =>
                        log.warning("Request to {} not authorized", uri)
                        val error: APIError = new APIError("Not authorized")
                        complete((Unauthorized, error))
                    }
                case e: NoSuchElementException =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("Object not found")
                        complete((NotFound, error))
                    }
                case e: EventNotFound =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("Event not found")
                        complete((NotFound, error))
                    }
                case e: UserAlreadyAdded =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("User already added to this event")
                        complete((Conflict, error))
                    }
                case e: UserNotPresent =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("User not part of this event")
                        complete((NotFound, error))
                    }
                case e: Exception =>
                    requestUri { uri =>
                        log.error(e, "Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError("Internal server error")
                        complete((InternalServerError, error))
                    }
            }
    }

}
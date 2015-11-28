import java.util._
import java.util.concurrent.TimeoutException

import _root_.directives.{InvalidCredentialsException, UnauthorizedException}
import akka.actor._
import auth.TokenAuthenticator
import auth.providers.FacebookProvider.InvalidTokenException
import model._
import model.user.User
import service.http._
import service.storage.auth.{UserExpiredException, AuthStorageService}
import service.storage.events.{EventHasOtherParticipants, EventNotFound, UserAlreadyAdded, UserNotPresent}
import spray.http.StatusCodes
import spray.http.StatusCodes._
import spray.routing._
import spray.routing.directives.AuthMagnet
import spray.util.LoggingContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App with SimpleRoutingApp with CORSSupport {

    implicit val system = ActorSystem("my-system")

    val authenticator1 = TokenAuthenticator[User](
        headerName = "token",
        queryStringParameterName = "token") { key =>
        Future {
            AuthStorageService.loadUserByToken(key)
        }
    }

    val events_service = new EventHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = system

        override implicit def authenticator: AuthMagnet[User] = authenticator1
    }

    val user_service = new UserHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = system

        override implicit def authenticator: AuthMagnet[User] = authenticator1
    }

    val token_service = new TokenHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = system
    }

    val tag_service = new TagHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = system
    }

    val doc_service = new DocHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = system
    }

    val admin_service = new AdminHTTPService {
        override implicit def actorRefFactory: ActorRefFactory = system
    }

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {
        cors {
            path("") {
                get {
                    complete("OK")
                }
            } ~ doc_service.routes() ~
                handleRejections(MyRejectionHandler.jsonRejectionHandler) {
                    handleExceptions(MyExceptionHandler.myExceptionHandler) {
                        admin_service.routes() ~ tag_service.public_routes() ~
                            token_service.routes() ~
                            events_service.routes() ~
                            user_service.routes()
                    }
                }
        }
    }

    object MyRejectionHandler {

        import format.ErrorJsonFormat._
        import spray.httpx.SprayJsonSupport._

        implicit val jsonRejectionHandler = RejectionHandler {
            case AuthenticationFailedRejection(msg, cause) :: _ =>
                complete((StatusCodes.Unauthorized, APIError("Authentication failure")))
            case MissingQueryParamRejection(name) :: _ =>
                complete((StatusCodes.BadRequest, APIError("Parameter missing: " + name)))
            case MissingFormFieldRejection(name) :: _ =>
                complete((StatusCodes.BadRequest, APIError("Parameter missing: " + name)))
            case MalformedRequestContentRejection(message, cause) :: _ =>
                complete((StatusCodes.BadRequest, APIError(message)))
            case MalformedQueryParamRejection(name, error, cause) :: _ =>
                complete((StatusCodes.BadRequest, APIError(error)))
            case UnsupportedRequestContentTypeRejection(msg) :: _ =>
                complete((StatusCodes.UnsupportedMediaType, APIError(msg)))
            case RequestEntityExpectedRejection :: _ =>
                complete((StatusCodes.UnsupportedMediaType, APIError("Wrong or missing content")));
            case _ :: _ =>
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
                        log.error(e, "Request to {} not found", uri)
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
                case e: EventHasOtherParticipants =>
                    requestUri { uri =>
                        log.warning("Request to {} not found", uri)
                        val error: APIError = new APIError("Can not delete event with participants")
                        complete((BadRequest, error))
                    }
                case e: InvalidTokenException =>
                    requestUri { uri =>
                        log.error(e, "Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError("Invalid token")
                        complete((Unauthorized, error))
                    }
                case e: com.mongodb.DuplicateKeyException =>
                    requestUri { uri =>
                        log.warning("Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError("Email already exists")
                        complete((Conflict, error))
                    }
                case e: UserExpiredException =>
                    requestUri { uri =>
                        log.warning("Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError("User did not confirmed within 4h. Recreate.")
                        complete((NotFound, error))
                    }
                case e: InvalidCredentialsException =>
                    requestUri { uri =>
                        log.warning("Request to {} could not be handled normally", uri)
                        val error: APIError = new APIError(e.msg)
                        complete((BadRequest, error))
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
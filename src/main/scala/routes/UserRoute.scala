package routes

import java.util.concurrent.TimeUnit

import _root_.directives.JsonUserDirective
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.wordnik.swagger.annotations.{Api, ApiOperation}
import model._
import service._
import spray.http.HttpHeaders.RawHeader
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global

object UserRoute extends UserRoute

@Api(value = "/user", description = "Operations about user.", produces = "application/json", position = 1)
class UserRoute extends SimpleRoutingApp {

    implicit val timeout = Timeout(20, TimeUnit.SECONDS)
    implicit val eventService = new EventService()
    implicit val system = ActorSystem("my-system")
    implicit val userService = system.actorOf(Props[UserService], "user-service")
    object toJson extends JsonUserDirective

    def route(user: User): Route = {
        path("user") {
            updateUser(user) ~ readUserByToken()
        } ~ pathPrefix("user" / Segment) {
            id =>
                pathEnd {
                    readUserById(id)
                } ~ path("events") {
                    listUserEvents(id)
                }
        }
    }

    @ApiOperation(httpMethod = "GET", value = "List user events")
    def listUserEvents(id: String): Route = {
        get {
            complete {
                toJson {
                    eventService.findEvents(id)
                }
            }
        }
    }

    @ApiOperation(httpMethod = "GET", value = "Get user by token")
    def readUserByToken(): Route = {
        import format.UserDataJsonFormat._
        import spray.httpx.SprayJsonSupport._
        get {
            parameters('token.as[String]) { token =>
                complete {
                    (userService ? GetUserByToken(token)).mapTo[Option[User]].map(result => {
                        toJson(result)
                    })
                }
            }
        }
    }

    @ApiOperation(httpMethod = "GET", value = "Get user by id")
    def readUserById(id: String): Route = {
        import format.UserDataJsonFormat._
        import spray.httpx.SprayJsonSupport._
        get {
            complete {
                (userService ? GetUserById(id)).mapTo[Option[User]].map(result => {
                    toJson(result)
                })
            }
        }
    }

    @ApiOperation(httpMethod = "PUT", value = "Update user")
    def updateUser(user: User): Route = {
        import format.APIResponseFormat._
        import format.UserDataJsonFormat._
        import spray.httpx.SprayJsonSupport._
        post {
            entity(as[UserData]) {
                userData =>
                    respondWithHeader(RawHeader("Location", user.id)) {
                        complete {
                            (userService ? UpdateUserData(user, userData)).mapTo[Option[User]].map(result => {
                                APIResponse("OK")
                            })
                        }
                    }
            }
        }
    }
}

package routes

import java.util.concurrent.TimeUnit

import _root_.directives.JsonUserDirective
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import model._
import service._
import spray.http.HttpHeaders.RawHeader
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global

case class UserRoute() {
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)
    implicit val eventService = new EventService()
    implicit val system = ActorSystem("my-system")
    implicit val userService = system.actorOf(Props[UserService], "user-service")

    object toJson extends JsonUserDirective

}

object UserRoute extends UserRoute with SimpleRoutingApp {

    def route(user: User): Route = {
        path("user") {
            updateUser(user)
        } ~ pathPrefix("user" / Segment) {
            id =>
                pathEnd {
                    readUser(id)
                } ~ path("events") {
                    listUserEvents(id)
                }
        }
    }

    def listUserEvents(id: String): Route = {
        get {
            complete {
                toJson {
                    eventService.findEvents(id)
                }
            }
        }
    }

    def readUser(id: String): Route = {
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

package service.http

import javax.ws.rs.Path

import _root_.directives.JsonUserDirective
import com.wordnik.swagger.annotations._
import model.event.Event
import model.user.{PublicUser, User}
import service.storage.events.EventStorageService
import service.storage.users.UserStorageService
import spray.routing._

@Api(value = "/user", description = "Operations about user.", produces = "application/json", position = 1)
trait UserHTTPService extends HttpService {

    implicit val eventService = new EventStorageService()

    object toJson extends JsonUserDirective

    def routes(user: User): Route = {
        pathPrefix("user" / Segment) {
            id =>
                pathEnd {
                    readUser(id) ~ updateUser(id, user.token)
                } ~ path("events") {
                    listUserEvents(id)
                }
        }
    }

    @Path("/{user_id}/events")
    @ApiOperation(
        httpMethod = "GET",
        value = "List user events",
        response = classOf[Event],
        responseContainer = "List")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "user_id",
            value = "User id",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def listUserEvents(id: String): Route = {
        get {
            complete {
                toJson {
                    eventService.findEvents(id)
                }
            }
        }
    }

    @Path("{user_id}")
    @ApiOperation(
        httpMethod = "GET",
        value = "Read user",
        response = classOf[PublicUser])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "user_id",
            value = "User id",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "token",
            value = "User token",
            required = true,
            dataType = "string",
            paramType = "query")
    ))
    def readUser(id: String): Route = {
        get {
            complete {
                import format.PublicUserJsonProtocol._
                import spray.httpx.SprayJsonSupport._
                UserStorageService.readPublicUserData(id)
            }
        }
    }

    @Path("{user_id}")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Update user")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "user_id",
            value = "User id",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "token",
            value = "User token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "user",
            value = "User data",
            required = true,
            dataType = "PublicUser",
            paramType = "body")
    ))
    def updateUser(id: String, token: String): Route = {
        import format.PublicUserJsonProtocol._
        import spray.httpx.SprayJsonSupport._
        put {
            entity(as[PublicUser]) {
                userData =>
                    complete {
                        UserStorageService.updateUser(id, token, userData)
                    }
            }
        }
    }
}

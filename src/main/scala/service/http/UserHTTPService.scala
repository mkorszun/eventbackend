package service.http

import javax.ws.rs.Path

import _root_.directives.{JsonUserDirective, UserPermissions}
import com.wordnik.swagger.annotations._
import config.Config
import model.APIResponse
import model.event.Event
import model.user.{PublicUser, User}
import service.photo.PhotoStorageService
import service.storage.events.EventStorageService
import service.storage.users.UserStorageService
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders.`Cache-Control`
import spray.http.{BodyPart, MultipartFormData}
import spray.routing._

@Api(value = "/user", description = "User actions", produces = "application/json", position = 1)
trait UserHTTPService extends HttpService with UserPermissions with Config {

    implicit val eventService = new EventStorageService()

    implicit def authenticator: spray.routing.directives.AuthMagnet[User]

    object toJson extends JsonUserDirective

    def routes(): Route = {
        authenticate(authenticator) { user =>
            pathPrefix("user" / Segment) {
                id =>
                    pathEnd {
                        readUser(id) ~ updateUser(id, user)
                    } ~ path("events") {
                        listUserEvents(id)
                    } ~ path("photo") {
                        updateUserPhoto(id, user)
                    }
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
            value = "User auth token",
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
            respondWithHeader(`Cache-Control`(`max-age`(MAX_AGE_USER_EVENTS))) {
                complete {
                    toJson {
                        eventService.findEvents(id)
                    }
                }
            }
        }
    }

    @Path("/{user_id}")
    @ApiOperation(
        httpMethod = "GET",
        value = "Read user",
        response = classOf[PublicUser])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "user_id",
            value = "User to read",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
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

    @Path("/{user_id}")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Update user")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "user_id",
            value = "User to update",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "user",
            value = "User to update",
            required = true,
            dataType = "PublicUser",
            paramType = "body")
    ))
    def updateUser(id: String, user: User): Route = {
        import format.PublicUserJsonProtocol._
        import spray.httpx.SprayJsonSupport._
        put {
            checkPermissions(id, user) {
                res => {
                    entity(as[PublicUser]) {
                        userData =>
                            complete {
                                UserStorageService.updateUser(id, user.token, userData)
                            }
                    }
                }
            }
        }
    }

    @Path("/{user_id}/photo")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Update user photo")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "user_id",
            value = "User to update",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "photo",
            value = "User photo",
            required = true,
            dataType = "File",
            paramType = "form")
    ))
    def updateUserPhoto(id: String, user: User): Route = {
        import format.APIResponseFormat._
        import spray.httpx.SprayJsonSupport._
        put {
            checkPermissions(id, user) {
                res => {
                    entity(as[MultipartFormData]) {
                        formData =>
                            complete {
                                val name: String = user.id + ".jpg"
                                val part: BodyPart = formData.get("photo").get
                                val bytes: Array[Byte] = part.entity.data.toByteArray
                                val path: String = PhotoStorageService.upload(name, bytes)
                                UserStorageService.updatePhoto(id, user.token, path)
                                APIResponse(path)
                            }
                    }
                }
            }
        }
    }
}

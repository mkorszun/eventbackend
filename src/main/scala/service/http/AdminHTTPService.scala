package service.http

import javax.ws.rs.Path

import _root_.directives.JsonUserDirective
import com.wordnik.swagger.annotations._
import config.Config
import model.APIResponse
import model.admin.AdminEvent
import service.storage.admin.AdminStorageService
import spray.routing
import spray.routing._
import spray.routing.authentication.BasicAuth

import scala.concurrent.ExecutionContext.Implicits.global

@Api(value = "/admin", description = "Admin actions", produces = "application/json", position = 1)
trait AdminHTTPService extends HttpService with Config with AdminStorageService {

    object toJson extends JsonUserDirective

    def routes(): Route = {
        authenticate(BasicAuth(realm = "Admin API")) { user =>
            pathPrefix("admin") {
                pathEnd {
                    complete("Admin interface")
                } ~ pathPrefix("event") {
                    pathEnd {
                        listEvents ~ createEvent
                    } ~ path(Segment) { id =>
                        readEvent(id) ~ updateEvent(id) ~ deleteEvent(id)
                    }
                } ~ pathPrefix("auth") {
                    auth
                }
            }
        }
    }

    @Path("/auth")
    @ApiOperation(
        httpMethod = "POST",
        value = "Auth admin")
    def auth: Route = {
        post {
            complete("OK")
        }
    }

    @Path("/event")
    @ApiOperation(
        httpMethod = "GET",
        value = "List events")
    def listEvents: Route = {
        get {
            complete {
                toJson {
                    adminList()
                }
            }
        }
    }

    @Path("/event")
    @ApiOperation(
        httpMethod = "POST",
        value = "Create event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "event",
            value = "Event to create",
            required = true,
            dataType = "model.admin.AdminEvent",
            paramType = "body")
    ))
    def createEvent: Route = {
        import format.APIResponseFormat._
        import format.AdminEventJsonFormat._
        import spray.httpx.SprayJsonSupport._
        post {
            entity(as[AdminEvent]) {
                event =>
                    complete {
                        toJson {
                            adminCreate(event)
                        }
                    }
            }
        }
    }

    @Path("/event/{event_id}")
    @ApiOperation(
        httpMethod = "GET",
        value = "Read event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to read",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def readEvent(id: String): Route = {
        get {
            complete {
                toJson {
                    adminRead(id)
                }
            }
        }
    }

    @Path("/event/{event_id}")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Update event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "event",
            value = "Event to update",
            required = true,
            dataType = "model.admin.AdminEvent",
            paramType = "body"),
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to update",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def updateEvent(id: String): Route = {
        import format.AdminEventJsonFormat._
        import spray.httpx.SprayJsonSupport._
        put {
            entity(as[AdminEvent]) {
                event =>
                    complete {
                        toJson {
                            adminUpdate(id, event)
                        }
                    }
            }
        }
    }

    @Path("/event/{event_id}")
    @ApiOperation(
        httpMethod = "DELETE",
        value = "Delete event",
        response = classOf[APIResponse])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to delete",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def deleteEvent(event_id: String): routing.Route = {
        import format.APIResponseFormat._
        import spray.httpx.SprayJsonSupport._
        delete {
            complete {
                toJson {
                    adminDelete(event_id)
                }
            }
        }
    }
}

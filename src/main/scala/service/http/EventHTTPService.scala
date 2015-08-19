package service.http

import javax.ws.rs.Path

import _root_.directives.JsonEventDirective
import com.wordnik.swagger.annotations._
import config.Config
import model.event.Event
import model.user.User
import service.storage.events.EventStorageService
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders.`Cache-Control`
import spray.routing
import spray.routing._

@Api(value = "/event", description = "Event actions", produces = "application/json", position = 1)
trait EventHTTPService extends HttpService with Config {

    implicit val eventService = new EventStorageService()

    object toJson extends JsonEventDirective

    def public_routes(): Route = {
        path("event") {
            listEvents
        }
    }

    def routes(user: User): Route = {
        path("event") {
            createEvent(user)
        } ~
            pathPrefix("event" / Segment) { id =>
                pathEnd {
                    updateEvent(id, user) ~ deleteEvent(id, user)
                }
            } ~
            pathPrefix("event" / Segment / "user") { id =>
                pathEnd {
                    joinEvent(user, id) ~ leaveEvent(user, id)
                }
            } ~
            pathPrefix("event" / Segment / "comment") { id =>
                pathEnd {
                    addComment(user, id)
                }
            }
    }

    @Path("/{event_id}/comment")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Comment event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to comment",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "msg",
            value = "Message",
            required = true,
            dataType = "string",
            paramType = "query")
    ))
    def addComment(user: User, id: String): Route = {
        put {
            parameters('msg.as[String]) {
                msg =>
                    complete {
                        toJson {
                            eventService.addComment(id, user, msg)
                        }
                    }
            }
        }
    }

    @Path("/{event_id}/user")
    @ApiOperation(
        httpMethod = "DELETE",
        value = "Leave event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to leave",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def leaveEvent(user: User, id: String): Route = {
        delete {
            complete {
                toJson {
                    eventService.removeParticipant(id, user)
                }
            }
        }
    }

    @Path("/{event_id}/user")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Join event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to join",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def joinEvent(user: User, id: String): Route = {
        put {
            complete {
                toJson {
                    eventService.addParticipant(id, user)
                }
            }
        }
    }

    @ApiOperation(
        httpMethod = "GET",
        value = "List events")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "x",
            value = "Latitude",
            required = true,
            dataType = "double",
            paramType = "query"),
        new ApiImplicitParam(
            name = "y",
            value = "Longitude",
            required = true,
            dataType = "double",
            paramType = "query"),
        new ApiImplicitParam(
            name = "max",
            value = "Max radius [m]",
            required = true,
            dataType = "integer",
            paramType = "query"),
        new ApiImplicitParam(
            name = "tags",
            value = "Comma separated tags",
            required = false,
            dataType = "string",
            paramType = "query")
    ))
    def listEvents: Route = {
        get {
            parameters('x.as[Double], 'y.as[Double], 'max.as[Long], 'tags.as[String] ?) {
                (x, y, max, t) =>
                    respondWithHeader(`Cache-Control`(`max-age`(MAX_AGE_SEARCH))) {
                        complete {
                            toJson {
                                eventService.findEvents(x, y, max, tags(t))
                            }
                        }
                    }
            }
        }
    }

    @ApiOperation(
        httpMethod = "POST",
        value = "Create event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "event",
            value = "Event to create",
            required = true,
            dataType = "model.event.Event",
            paramType = "body")
    ))
    def createEvent(user: User): routing.Route = {
        import format.APIResponseFormat._
        import format.EventJsonFormat._
        import spray.httpx.SprayJsonSupport._
        post {
            entity(as[Event]) {
                event =>
                    complete {
                        toJson {
                            eventService.saveEvent(user, event)
                        }
                    }
            }
        }
    }

    @Path("/{event_id}")
    @ApiOperation(
        httpMethod = "PUT",
        value = "Update event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to update",
            required = true,
            dataType = "string",
            paramType = "path"),
        new ApiImplicitParam(
            name = "event",
            value = "Updated event",
            required = true,
            dataType = "model.event.Event",
            paramType = "body")
    ))
    def updateEvent(event_id: String, user: User): routing.Route = {
        import format.EventJsonFormat._
        import spray.httpx.SprayJsonSupport._
        put {
            entity(as[Event]) {
                event =>
                    complete {
                        toJson {
                            eventService.updateEvent(event_id, user, event)
                        }
                    }
            }
        }
    }

    @Path("/{event_id}")
    @ApiOperation(
        httpMethod = "DELETE",
        value = "Delete event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "token",
            value = "User auth token",
            required = true,
            dataType = "string",
            paramType = "query"),
        new ApiImplicitParam(
            name = "event_id",
            value = "Event to delete",
            required = true,
            dataType = "string",
            paramType = "path")
    ))
    def deleteEvent(event_id: String, user: User): routing.Route = {
        import format.APIResponseFormat._
        import spray.httpx.SprayJsonSupport._
        delete {
            complete {
                toJson {
                    eventService.deleteEvent(event_id, user)
                }
            }
        }
    }

    private def tags(t: Option[String]): Array[String] = {
        return if (t.nonEmpty) t.get.split(",+") else Array.empty[String]
    }
}

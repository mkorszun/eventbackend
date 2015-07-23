package service.http

import javax.ws.rs.Path

import _root_.directives.JsonEventDirective
import com.wordnik.swagger.annotations._
import model.{Event, User}
import service.storage.EventStorageService
import spray.routing
import spray.routing._

@Api(value = "/event", description = "Operations about events.", produces = "application/json", position = 1)
trait EventHTTPService extends HttpService {

    implicit val eventService = new EventStorageService()

    object toJson extends JsonEventDirective

    def routes(user: User): Route = {
        path("event") {
            createEvent(user) ~ listEvents
        } ~
            pathPrefix("event" / Segment) { id =>
                pathEnd {
                    updateEvent(id, user) ~ deleteEvent(id, user)
                }
            } ~
            pathPrefix("event" / Segment / "user") { id =>
                pathEnd {
                    addParticipant(user, id) ~ removeParticipant(user, id)
                }
            } ~
            pathPrefix("event" / Segment / "comment" / Segment) { (id, msg) =>
                pathEnd {
                    addComment(user, id, msg)
                }
            }
    }

    @Path("/{event_id}/comment/{msg}")
    @ApiOperation(httpMethod = "PUT", value = "Add comment to the event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "event_id", value = "Event id", required = true, dataType = "string", paramType = "path"),
        new ApiImplicitParam(name = "msg", value = "Message", required = true, dataType = "string", paramType = "path")
    ))
    def addComment(user: User, id: String, msg: String): Route = {
        put {
            complete {
                toJson {
                    eventService.addComment(id, user, msg)
                }
            }
        }
    }

    @Path("/{event_id}/user")
    @ApiOperation(httpMethod = "DELETE", value = "Remove self from the event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "event_id", value = "Event id", required = true, dataType = "string", paramType = "path")
    ))
    def removeParticipant(user: User, id: String): Route = {
        delete {
            complete {
                toJson {
                    eventService.removeParticipant(id, user)
                }
            }
        }
    }

    @Path("/{event_id}/user")
    @ApiOperation(httpMethod = "PUT", value = "Add self to the event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "event_id", value = "Event id", required = true, dataType = "string", paramType = "path")
    ))
    def addParticipant(user: User, id: String): Route = {
        put {
            complete {
                toJson {
                    eventService.addParticipant(id, user)
                }
            }
        }
    }

    @ApiOperation(httpMethod = "GET", value = "List events")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "x", value = "Latitude", required = true, dataType = "double", paramType = "query"),
        new ApiImplicitParam(name = "y", value = "Longitude", required = true, dataType = "double", paramType = "query"),
        new ApiImplicitParam(name = "max", value = "Max radius [m]", required = true, dataType = "integer", paramType = "query"),
        new ApiImplicitParam(name = "tags", value = "Comma separated tags", required = false, dataType = "string", paramType = "query")
    ))
    def listEvents: Route = {
        get {
            parameters('x.as[Double], 'y.as[Double], 'max.as[Long], 'tags.as[String] ?) {
                (x, y, max, t) =>
                    complete {
                        toJson {
                            eventService.findEvents(x, y, max, tags(t))
                        }
                    }
            }
        }
    }

    @ApiOperation(httpMethod = "POST", value = "Create event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "event", value = "New event", required = true, dataType = "model.Event", paramType = "body")
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
    @ApiOperation(httpMethod = "PUT", value = "Update event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "event_id", value = "Event id", required = true, dataType = "string", paramType = "path"),
        new ApiImplicitParam(name = "event", value = "Updated event", required = true, dataType = "model.Event", paramType = "body")
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
    @ApiOperation(httpMethod = "DELETE", value = "Delete event")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "token", value = "User token", required = true, dataType = "string", paramType = "query"),
        new ApiImplicitParam(name = "event_id", value = "Event id", required = true, dataType = "string", paramType = "path")
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

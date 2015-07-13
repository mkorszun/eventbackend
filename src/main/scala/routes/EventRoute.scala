package routes

import _root_.directives.JsonEventDirective
import com.wordnik.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import model.{Event, User}
import service.EventService
import spray.routing
import spray.routing._

object EventRoute extends EventRoute

@Api(value = "/event", description = "Operations about events.", produces = "application/json", position = 1)
class EventRoute extends SimpleRoutingApp {

    implicit val eventService = new EventService()
    object toJson extends JsonEventDirective

    def route(user: User): Route = {
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

    @ApiOperation(httpMethod = "PUT", value = "Adds comment")
    def addComment(user: User, id: String, msg: String): Route = {
        put {
            complete {
                toJson {
                    eventService.addComment(id, user, msg)
                }
            }
        }
    }

    @ApiOperation(httpMethod = "DELETE", value = "Remove participant")
    def removeParticipant(user: User, id: String): Route = {
        delete {
            complete {
                toJson {
                    eventService.removeParticipant(id, user)
                }
            }
        }
    }

    @ApiOperation(httpMethod = "PUT", value = "Add participant")
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

    @ApiOperation(httpMethod = "PUT", value = "Update event")
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

    @ApiOperation(httpMethod = "DELETE", value = "Delete event")
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

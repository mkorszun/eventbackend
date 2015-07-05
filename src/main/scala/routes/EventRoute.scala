package routes

import _root_.directives.JsonEventDirective
import model.{Event, User}
import service.EventService
import spray.routing
import spray.routing._

case class EventRoute() {
    implicit val eventService = new EventService()

    object toJson extends JsonEventDirective

}

object EventRoute extends EventRoute with SimpleRoutingApp {

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

    def addComment(user: User, id: String, msg: String): Route = {
        put {
            complete {
                toJson {
                    eventService.addComment(id, user, msg)
                }
            }
        }
    }

    def removeParticipant(user: User, id: String): Route = {
        delete {
            complete {
                toJson {
                    eventService.removeParticipant(id, user)
                }
            }
        }
    }

    def addParticipant(user: User, id: String): Route = {
        put {
            complete {
                toJson {
                    eventService.addParticipant(id, user)
                }
            }
        }
    }

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

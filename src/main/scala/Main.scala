import akka.actor._
import com.mongodb.DBCursor
import model.Event
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp {

    implicit val system = ActorSystem("my-system")
    implicit val dbService = new db.DBService()

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {

        import format.EventJsonFormat._
        import spray.httpx.SprayJsonSupport._

        path("") {
            get {
                complete("OK")
            }
        } ~
            path("event") {
                post {
                    entity(as[Event]) {
                        event =>
                            dbService.saveEvent(event)
                            complete("OK")
                    }
                } ~ get {
                    parameters('x.as[Double], 'y.as[Double], 'max.as[Long]) {
                        (x, y, max) =>
                            val events: DBCursor = dbService.findEvents(x, y, max)
                            complete(dbService.toJson(events))
                    }
                }
            }
    }
}


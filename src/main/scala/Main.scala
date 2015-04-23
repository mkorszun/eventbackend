import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import auth.TokenAuthenticator
import com.mongodb.DBCursor
import model.{Event, User}
import service.{GetUser, UserService}
import spray.routing.SimpleRoutingApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object Main extends App with SimpleRoutingApp {

    implicit val system = ActorSystem("my-system")
    implicit val dbService = new db.DBService()
    implicit val userService = system.actorOf(Props[UserService], "user-service")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    val authenticator = TokenAuthenticator[User](
        headerName = "token",
        queryStringParameterName = "token"
    ) { key =>
        val future = userService ? GetUser(key)
        val user = Await.result(future, timeout.duration).asInstanceOf[Option[User]]
        Future(user)
    }

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {

        import format.EventJsonFormat._
        import spray.httpx.SprayJsonSupport._

        path("") {
            get {
                complete("OK")
            }
        } ~
            authenticate(authenticator) { user =>
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
}
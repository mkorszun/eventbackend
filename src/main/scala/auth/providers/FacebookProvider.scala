package auth.providers

import akka.actor.ActorSystem
import model.token.Token
import model.user.User
import service.storage.users.UserStorageService
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.marshalling.ToResponseMarshallable
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

object FacebookProvider {

    case class FacebookUser(id: String, first_name: String, last_name: String)

    object FacebookUserJsonProtocol extends DefaultJsonProtocol {
        implicit val facebookUserFormat = jsonFormat3(FacebookUser)
    }

    val accept = "application/json; charset=UTF-8"

    import auth.providers.FacebookProvider.FacebookUserJsonProtocol._
    import format.TokenJsonProtocol._

    implicit val system = ActorSystem()

    import auth.providers.FacebookProvider.system.dispatcher

    def mapTextPlainToApplicationJson: HttpResponse => HttpResponse = {
        case r@HttpResponse(_, entity, _, _) =>
            r
                .withEntity(entity
                .flatMap(amazonEntity => HttpEntity(ContentType(MediaTypes.`application/json`), amazonEntity.data)))
        case x => x
    }

    def getOrCreate(token: String): ToResponseMarshallable = {
        val pipeline: HttpRequest => Future[FacebookUser] = (
            sendReceive ~> mapTextPlainToApplicationJson ~> unmarshal[FacebookUser])
        val response: Future[FacebookUser] = {
            pipeline(Get("https://graph.facebook.com/me?access_token=" + token))
        }

        val resp: Future[Future[Option[Token]]] = response.map[Future[Option[Token]]] {
            case FacebookUser(id, first_name, last_name) =>
                Future {
                    val newUser: User = User(
                        java.util.UUID.randomUUID.toString, id, "facebook", java.util.UUID.randomUUID.toString,
                        first_name, last_name, photo_link(id), "",  None, None, None)
                    Option(UserStorageService.createUser(newUser))
                }
            case _ =>
                Future {
                    None
                }
        }

        resp
    }

    def photo_link(user_id: String): String = {
        return f"https://graph.facebook.com/$user_id%s/picture?height=400&width=400"
    }
}
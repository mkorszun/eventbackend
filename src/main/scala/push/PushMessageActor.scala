package push

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging, Props}
import com.mongodb
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import model.user.User
import service.aws.SNSClient
import service.storage.events.EventStorageService
import service.storage.events.EventStorageService._
import service.storage.users.UserStorageService
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class NewParticipant(user: User, event_id: String, event: DBObject)

case class LeavingParticipant(user: User, event_id: String, event: DBObject)

case class NewComment(user: User, event_id: String)

case class EventChanged(user: User, event_id: String)

class PushMessageActor extends Actor with ActorLogging with SNSClient {

    val deviceActor = context.actorOf(Props[DeviceRegistrationActor])
    val executorService = Executors.newFixedThreadPool(PUSH_EXECUTOR_SIZE)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case NewParticipant(user, event_id, event) =>
            log.info(f"New participant for event: $event_id")
            notifyOwner(user, event_id, PushType.new_participant, event)
        case NewComment(user, event_id) =>
            log.info(f"New comment for event: $event_id")
            notifyParticipants(user, event_id, PushType.new_comment)
        case EventChanged(user, event_id) =>
            log.info(f"Event $event_id changed")
            notifyParticipants(user, event_id, PushType.event_updated)
        case LeavingParticipant(user, event_id, event) =>
            log.info(f"Leaving participant for event: $event_id")
            notifyOwner(user, event_id, PushType.leaving_participant, event)
    }

    private def notifyParticipants(user: User, id: String, msg_type: PushType.PushType): Unit = {
        val f = Future {

            val fields = MongoDBObject("participants" -> "$participants.id",
                "headline" -> "$headline", "updated_at" -> "$updated_at",
                "comments_count" -> "$comments_count", "timestamp" -> "$timestamp")

            val cursor = EventStorageService.aggregate(aggregationSteps(Array(
                MongoDBObject("$match" -> MongoDBObject("_id" -> id)),
                MongoDBObject("$project" -> fields)
            )))

            val results: mongodb.DBObject = cursor.next()
            cursor.close()

            val participants = toArray(results.get("participants").asInstanceOf[BasicDBList])
            val headline = results.getAs[String]("headline").get
            val updated_at = results.getAs[Long]("updated_at").get
            val comments_count = results.getAs[Int]("comments_count").get
            val timestamp = results.getAs[Long]("timestamp").get

            val default = alert(msg_type, headline, user.fullName)
            val params = new Params(id, headline, msg_type.toString, user.fullName, updated_at, comments_count, timestamp)
            val APNS = new APNS(new APS(1, default, "default"), params).toJson.toString()
            val GCM = new GCM(new DATA(default, params)).toJson.toString()
            val payload = new PushMessage(default, GCM, APNS, APNS)

            // iOS - only if settings enabled
            for (token <- UserStorageService.getUserDevices(participants, msg_type, "APNS") diff user.devices.get) {
                if (!push(token, payload.toJson.toString())) deviceActor ! UnregisterDevice(null, token)
            }

            // Android - all users
            for (token <- UserStorageService.getUserDevices(participants, "GCM") diff user.devices.get) {
                if (!push(token, payload.toJson.toString())) deviceActor ! UnregisterDevice(null, token)
            }
        }

        f onFailure {
            case e =>
                log.error(e, f"Failed to send push message: $msg_type for event: $id")
        }
    }

    private def notifyOwner(user: User, id: String, msg_type: PushType.PushType, event: DBObject): Unit = {
        val f = Future {

            val headline = event.get("headline").asInstanceOf[String]
            val updated_at = event.get("updated_at").asInstanceOf[Long]
            val comments_count = event.get("comments_count").asInstanceOf[Int]
            val timestamp = event.get("timestamp").asInstanceOf[Long]
            val owner_id = event.get("user").asInstanceOf[DBObject].get("id").asInstanceOf[String]

            val default = alert(msg_type, headline, user.fullName)
            val params = new Params(id, headline, msg_type.toString, user.fullName, updated_at, comments_count, timestamp)
            val APNS = new APNS(new APS(1, default, "default"), params).toJson.toString()
            val GCM = new GCM(new DATA(default, params)).toJson.toString()
            val payload = new PushMessage(default, GCM, APNS, APNS)

            // iOS - only if settings enabled
            for (token <- UserStorageService.getUserDevices(Array(owner_id), msg_type, "APNS") diff user.devices.get) {
                if (!push(token, payload.toJson.toString())) deviceActor ! UnregisterDevice(null, token)
            }

            // Android - all users
            for (token <- UserStorageService.getUserDevices(Array(owner_id), "GCM") diff user.devices.get) {
                if (!push(token, payload.toJson.toString())) deviceActor ! UnregisterDevice(null, token)
            }
        }

        f onFailure {
            case e =>
                log.error(e, f"Failed to send push message: $msg_type for event: $id")
        }
    }

    private def alert(msg_type: PushType.PushType, event_name: String,
        participant_name: String): String = msg_type match {
        case PushType.new_participant => String.format(PUSH_MSG_NEW_PARTICIPANT, participant_name, event_name)
        case PushType.new_comment => String.format(PUSH_MSG_NEW_COMMENT, participant_name, event_name)
        case PushType.leaving_participant => String.format(PUSH_MSG_LEAVING_PARTICIPANT, participant_name, event_name)
        case PushType.event_updated => String.format(PUSH_MSG_UPDATED_EVENT, participant_name, event_name)
    }
}

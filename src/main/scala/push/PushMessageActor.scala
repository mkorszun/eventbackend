package push

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging, Props}
import model.user.User
import service.aws.SNSClient
import service.storage.events.EventStorageService
import service.storage.users.UserStorageService
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class NewParticipant(user: User, event_id: String)

case class LeavingParticipant(user: User, event_id: String)

case class NewComment(user: User, event_id: String)

case class EventChanged(user: User, event_id: String)

class PushMessageActor extends Actor with ActorLogging with SNSClient {

    val deviceActor = context.actorOf(Props[DeviceRegistrationActor])
    val executorService = Executors.newFixedThreadPool(PUSH_EXECUTOR_SIZE)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case NewParticipant(user, event_id) =>
            log.info(f"New participant for event: $event_id")
            notifyParticipants(user, event_id, PushType.new_participant)
        case NewComment(user, event_id) =>
            log.info(f"New comment for event: $event_id")
            notifyParticipants(user, event_id, PushType.new_comment)
        case EventChanged(user, event_id) =>
            log.info(f"Event $event_id changed")
            notifyParticipants(user, event_id, PushType.event_updated)
        case LeavingParticipant(user, event_id) =>
            log.info(f"Leaving participant for event: $event_id")
            notifyParticipants(user, event_id, PushType.leaving_participant)
    }

    private def notifyParticipants(user: User, event_id: String, msg_type: PushType.PushType): Unit = {
        val f = Future {
            val result: EventStorageService.GroupResult = EventStorageService.getEventParticipants(event_id)
            for (token <- UserStorageService.getUserDevices(result.res1, msg_type) diff user.devices.get) {
                val default = alert(msg_type, result.res2, user.fullName)
                val params = new Params(event_id, result.res2, msg_type.toString, user.fullName)
                val APNS = new APNS(new APS(1, default, "default"), params)
                val GCM = new GCM(new DATA(default, params))
                val payload = new PushMessage(default, GCM.toJson.toString(), APNS.toJson.toString())
                if (!push(token, payload.toJson.toString())) deviceActor ! UnregisterDevice(null, token)
            }
        }

        f onFailure {
            case e =>
                log.error(e, f"Failed to send push message: $msg_type for event: $event_id")
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

package push

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging}
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

    val executorService = Executors.newFixedThreadPool(PUSH_EXECUTOR_SIZE)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case NewParticipant(user, event_id) =>
            log.info(f"New participant for event: $event_id")
            notifyParticipants(user, event_id, "new_participant")
        case NewComment(user, event_id) =>
            log.info(f"New comment for event: $event_id")
            notifyParticipants(user, event_id, "new_comment")
        case EventChanged(user, event_id) =>
            log.info(f"Event $event_id changed")
            notifyParticipants(user, event_id, "event_updated")
        case LeavingParticipant(user, event_id) =>
            log.info(f"Leaving participant for event: $event_id")
            notifyParticipants(user, event_id, "leaving_participant")
    }

    private def notifyParticipants(user: User, event_id: String, msg_type: String): Unit = {
        val f = Future {
            val result: EventStorageService.GroupResult = EventStorageService.getEventParticipants(event_id)
            for (token <- UserStorageService.getUserDevices(result.res1, msg_type) diff user.devices.get) {
                val message: PushMessage = PushMessage(event_id, result.res2, msg_type, user.fullName)
                val payload = PushMessageWrapper2(PushMessageWrapper1("1", message))
                push(token, payload.toJson.toString())
            }
        }

        f onFailure {
            case e =>
                log.error(e, f"Failed to send push message: $msg_type for event: $event_id")
        }
    }
}

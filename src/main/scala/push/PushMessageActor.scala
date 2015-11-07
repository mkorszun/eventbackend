package push

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging}
import service.aws.SNSClient
import service.storage.events.EventStorageService
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class NewParticipant(event_id: String)

case class NewComment(event_id: String)

case class EventChanged(event_id: String)

class PushMessageActor extends Actor with ActorLogging with SNSClient {

    val executorService = Executors.newFixedThreadPool(PUSH_EXECUTOR_SIZE)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case NewParticipant(event_id) =>
            log.info(f"New participant for event: $event_id")
            notifyParticipants(event_id, "new_participant")
        case NewComment(event_id) =>
            log.info(f"New comment for event: $event_id")
            notifyParticipants(event_id, "new_comment")
        case EventChanged(event_id) =>
            log.info(f"Event $event_id changed")
            notifyParticipants(event_id, "event_updated")
    }

    private def notifyParticipants(event_id: String, msg: String): Unit = {
        val f = Future {
            val result: EventStorageService.GroupResult = EventStorageService.getEventDevices(event_id)
            for (token <- result.res1) push(token, PushMessage(event_id, result.res2, msg).toJson.toString())
        }

        f onFailure {
            case e =>
                log.error(e, f"Failed to send push message: $msg for event: $event_id")
        }
    }
}

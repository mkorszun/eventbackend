package push

import akka.actor.{Actor, ActorLogging}

case class NewParticipant(event_id: String)

case class NewComment(event_id: String)

case class EventChanged(event_id: String)

class PushMessageActor extends Actor with ActorLogging {

    override def receive: Receive = {
        case NewParticipant(event_id) =>
            log.info(f"New participant for event: $event_id")
        case NewComment(event_id) =>
            log.info(f"New comment for event: $event_id")
        case EventChanged(event_id) =>
            log.info(f"Event $event_id changed")
    }
}

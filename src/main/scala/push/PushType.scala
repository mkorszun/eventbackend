package push

object PushType extends Enumeration {
    type PushType = Value
    val new_participant, new_comment, event_updated, leaving_participant = Value
}

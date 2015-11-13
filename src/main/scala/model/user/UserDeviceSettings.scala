package model.user

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.wordnik.swagger.annotations.ApiModel

@ApiModel(description = "User device settings")
case class UserDeviceSettings(
    push_on_new_comment: Boolean = true,
    push_on_new_participant: Boolean = true,
    push_on_update: Boolean = true)

object UserDeviceSettings {

    private val push_on_new_comment: String = "push_on_new_comment"
    private val push_on_new_participant: String = "push_on_new_participant"
    private val push_on_update: String = "push_on_update"

    def fromDocument(document: DBObject): UserDeviceSettings = {
        UserDeviceSettings(
            document.get(push_on_new_comment).asInstanceOf[Boolean],
            document.get(push_on_new_participant).asInstanceOf[Boolean],
            document.get(push_on_update).asInstanceOf[Boolean]
        )
    }

    def toDocument(obj: UserDeviceSettings): DBObject = {
        MongoDBObject(
            push_on_new_comment -> obj.push_on_new_comment,
            push_on_new_participant -> obj.push_on_new_participant,
            push_on_update -> obj.push_on_update
        )
    }
}

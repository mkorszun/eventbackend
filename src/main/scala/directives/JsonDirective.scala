package directives

import java.util

import com.mongodb.DBCursor
import com.mongodb.casbah.Imports._
import com.stormpath.sdk.directory.CustomData
import model.{APIResponse, User, UserData}
import spray.routing.directives.RouteDirectives

class JsonDirective extends RouteDirectives {
    def apply(results: DBCursor): String = {
        com.mongodb.util.JSON.serialize(results)
    }

    def apply(result: DBObject): String = {
        com.mongodb.util.JSON.serialize(result)
    }

    def apply(result: Unit): APIResponse = {
        APIResponse("OK")
    }
}

class JsonEventDirective extends JsonDirective

class JsonUserDirective extends JsonDirective {
    def apply(result: Option[User]): UserData = {
        apply(result.get)
    }

    def apply(result: User): UserData = {
        val id: String = result.id
        val customData: CustomData = result.account.getCustomData
        val photo_url: String = customData.get("photo_url").toString
        val age: Int = customData.get("age").toString.toInt
        val bio: String = customData.get("bio").toString
        val tags: util.ArrayList[String] = customData.get("tags").asInstanceOf[util.ArrayList[String]]
        val firstName: String = customData.get("first_name").toString
        val lastName: String = customData.get("last_name").toString
        UserData(id, photo_url, age, bio, tags.toArray(new Array[String](tags.size())), firstName, lastName)
    }
}
package db

import com.mongodb.BasicDBObject
import com.mongodb.casbah.commons.MongoDBList
import com.stormpath.sdk.directory.CustomData
import model.{Event, User}

class DBEvent(user: User, event: Event) extends BasicDBObject {
    put("_id", java.util.UUID.randomUUID.toString)
    put("user", new DBUser(user))
    put("spots", event.spots)
    put("date_and_time", event.date_and_time)
    put("headline", event.headline)
    put("cost", event.cost)
    put("duration", event.duration)
    put("description", event.description)
    put("participants", new MongoDBList())
    put("comments", new MongoDBList())
    put("loc", new DBGeoPoint(event.x, event.y))
    put("tags", event.tags)
}

class DBUser(user: User) extends BasicDBObject {
    val data: CustomData = user.account.getCustomData
    put("id", user.id)
    put("photo_url", data.get("photo_url"))
    put("age", data.get("age"))
    put("bio", data.get("bio"))
}

class DBGeoPoint(x: Double, y: Double) extends BasicDBObject {
    put("type", "Point")
    put("coordinates", Array(x, y))
}

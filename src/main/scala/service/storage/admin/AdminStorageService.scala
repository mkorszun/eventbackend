package service.storage.admin

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{BasicDBObject, DBCollection, DBCursor}
import model.admin.AdminEvent
import service.storage.events.{EventHasOtherParticipants, EventNotFound}
import service.storage.users.UserStorageService

trait AdminStorageService {

    val collection = getCollection()

    def adminCreate(event: AdminEvent): Unit = {
        collection.insert(new DBAdminEvent(event))
    }

    def adminList(): DBCursor = {
        return collection.find(MongoDBObject(), MongoDBObject("comments" -> 0, "participants" -> 0))
    }

    def adminUpdate(event_id: String, event: AdminEvent): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound

        val update = $set(
            "headline" -> event.headline,
            "description" -> event.description,
            "timestamp" -> event.timestamp,
            "loc" -> new DBGeoPoint(event.x, event.y),
            "tags" -> event.tags,
            "distance" -> event.distance,
            "pace" -> event.pace,
            "user" -> UserStorageService.publicUserToDocument(event.user)
        )

        return collection.findAndModify(MongoDBObject("_id" -> event_id), null, null, false, update, true, false)
    }

    def adminDelete(event_id: String): Unit = {
        if (!isEvent(event_id)) throw new EventNotFound
        val doc = collection.findAndRemove(MongoDBObject("_id" -> event_id))
        if (doc == null) throw new EventHasOtherParticipants
    }

    // Helpers =======================================================================================================//

    private def isEvent(id: String): Boolean = {
        return if (collection.find(MongoDBObject("_id" -> id)).limit(1).size() == 0) false else true
    }

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("users")
    }

    // DB document objects ===========================================================================================//

    class DBGeoPoint(x: Double, y: Double) extends MongoDBObject {
        put("type", "Point")
        put("coordinates", GeoCoords(y, x))
    }

    private class DBAdminEvent(event: AdminEvent) extends BasicDBObject {
        put("_id", java.util.UUID.randomUUID.toString)
        put("user", UserStorageService.publicUserToDocument(event.user))
        put("timestamp", event.timestamp)
        put("headline", event.headline)
        put("description", event.description)
        put("participants", new MongoDBList())
        put("comments", new MongoDBList())
        put("loc", new DBGeoPoint(event.x, event.y))
        put("tags", event.tags)
        put("distance", event.distance)
        put("pace", event.pace)
        put("deleted", false)
        put("spots", 0)
    }

    //================================================================================================================//
}

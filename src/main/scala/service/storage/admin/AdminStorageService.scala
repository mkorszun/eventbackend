package service.storage.admin

import java.util.Date

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.{DBCursor, DBObject}
import model.admin.AdminEvent
import model.user.PublicUser
import service.storage.events.EventNotFound
import service.storage.utils.Storage

trait AdminStorageService extends Storage {

    val collection = getCollection("events")

    def adminList(): DBCursor = {
        return collection.find(MongoDBObject(), EVENT_LIST_ADMIN_FIELDS)
    }

    def adminCreate(event: AdminEvent): Unit = {
        collection.insert(new DBAdminEvent(event))
    }

    def adminRead(event_id: String): DBObject = {
        val query = MongoDBObject("_id" -> event_id)
        val doc = collection.findOne(query, EVENT_DETAILS_ADMIN_FIELDS)
        if (doc != null) return doc
        throw new EventNotFound
    }

    def adminDelete(event_id: String): Unit = {
        if (collection.findAndRemove(MongoDBObject("_id" -> event_id)) == null) throw new EventNotFound
    }

    def adminUpdate(event_id: String, event: AdminEvent): DBObject = {
        val update = $set(
            "headline" -> event.headline,
            "description" -> event.description,
            "timestamp" -> event.timestamp,
            "loc" -> new DBGeoPoint(event.x, event.y),
            "tags" -> event.tags,
            "distance" -> event.distance,
            "pace" -> event.pace,
            "www" -> event.www,
            "user.first_name" -> event.user.first_name,
            "user.www" -> event.user.www,
            "user.email" -> event.user.email,
            UPDATED_AT -> new Date().getTime
        )

        val query = MongoDBObject("_id" -> event_id)
        val doc = collection.findAndModify(query, EVENT_DETAILS_ADMIN_FIELDS, null, false, update, true, false)
        if (doc != null) return doc
        throw new EventNotFound
    }

    // DB document objects ===========================================================================================//

    private class DBAdminEvent(event: AdminEvent) extends BasicDBObject {
        put("_id", java.util.UUID.randomUUID.toString)
        put("user", PublicUser.toDocument(event.user))
        put("timestamp", event.timestamp)
        put("headline", event.headline)
        put("description", event.description)
        put("participants", new MongoDBList())
        put("comments", new MongoDBList())
        put(COMMENTS_COUNT, 0)
        put("loc", new DBGeoPoint(event.x, event.y))
        put("tags", event.tags)
        put("distance", event.distance)
        put("pace", event.pace)
        put("deleted", false)
        put("spots", 0)
        put("official", true)
        put("www", event.www)
        put(UPDATED_AT, new Date().getTime)
    }

    //================================================================================================================//
}

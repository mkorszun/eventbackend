package service.storage.events

import java.util.{Calendar, Date}

import com.mongodb._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBList, MongoDBListBuilder, MongoDBObject}
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import model.event.Event
import model.user.User
import service.storage.users.UserStorageService

class EventStorageService {

    val collection = getCollection()

    // Public API ====================================================================================================//

    def saveEvent(user: User, event: Event) {
        collection.insert(new DBEvent(user, event))
    }

    def findEvents(user_id: String): DBCursor = {
        return collection.find($or("user.id" -> user_id, "participants.id" -> user_id))
    }

    def findEvents(x: Double, y: Double, max: Long, tags: Array[String]): DBCursor = {
        val geo = MongoDBObject("$geometry" -> new DBGeoPoint(x, y), "$maxDistance" -> max)
        val timestamp = MongoDBObject("$gte" -> Calendar.getInstance().getTime().getTime)
        val query = MongoDBObject("loc" -> MongoDBObject("$near" -> geo), "timestamp" -> timestamp)
        if (tags.length > 0) query.put("tags", MongoDBObject("$elemMatch" -> MongoDBObject("$in" -> tags)))
        return collection.find(query).limit(50)
    }

    def removeParticipant(event_id: String, user: User): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> user.id)
        val participant = MongoDBObject("participants" -> MongoDBObject("id" -> user.id))
        val update = MongoDBObject("$pull" -> participant)
        val doc = collection.findAndModify(event, null, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserNotPresent
    }

    def addParticipant(event_id: String, user: User): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val constraint = MongoDBObject("$nin" -> (MongoDBList.newBuilder[String] += user.id).result())
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> constraint)
        val participant = MongoDBObject("participants" -> UserStorageService.userToParticipantDocument(user))
        val update = MongoDBObject("$addToSet" -> participant)
        val doc = collection.findAndModify(event, null, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserAlreadyAdded
    }

    def addComment(event_id: String, user: User, msg: String): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> user.id)
        val update = MongoDBObject("$push" -> MongoDBObject("comments" -> new DBEventComment(user, msg)))
        val doc = collection.findAndModify(event,  MongoDBObject("comments" -> 1, "_id" -> 0), null, false, update, true, false)
        if (doc != null) return doc
        throw new UserNotPresent
    }

    def updateEvent(event_id: String, user: User, event: Event): DBObject = {
        if (!isEvent(event_id, user)) throw new EventNotFound

        val update = $set(
            "headline" -> event.headline,
            "description" -> event.description,
            "timestamp" -> event.timestamp,
            "loc" -> new DBGeoPoint(event.x, event.y),
            "tags" -> event.tags,
            "distance" -> event.distance,
            "pace" -> event.pace
        )

        return collection.findAndModify(MongoDBObject("_id" -> event_id), null, null, false, update, true, false)
    }

    def deleteEvent(event_id: String, user: User): Unit = {
        if (!isEvent(event_id, user)) throw new EventNotFound
        collection.remove(MongoDBObject("_id" -> event_id, "user.id" -> user.id))
    }

    // Helpers =======================================================================================================//

    private def isEvent(id: String): Boolean = {
        return if (collection.find(MongoDBObject("_id" -> id)).limit(1).size() == 0) false else true
    }

    private def isEvent(id: String, user: User): Boolean = {
        val query: Imports.DBObject = MongoDBObject("_id" -> id, "user.id" -> user.id)
        return if (collection.find(query).limit(1).size() == 0) false else true
    }

    private def initialParticipants(user: User): MongoDBList = {
        val builder: MongoDBListBuilder = MongoDBList.newBuilder[DBObject]
        builder += UserStorageService.userToParticipantDocument(user)
        return builder.result()
    }

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }

    // DB document objects ===========================================================================================//

    private class DBGeoPoint(x: Double, y: Double) extends MongoDBObject {
        put("type", "Point")
        put("coordinates", GeoCoords(y, x))
    }

    private class DBEvent(user: User, event: Event) extends BasicDBObject {
        put("_id", java.util.UUID.randomUUID.toString)
        put("user", UserStorageService.userToPublicDocument(user))
        put("timestamp", event.timestamp)
        put("headline", event.headline)
        put("description", event.description)
        put("participants", initialParticipants(user))
        put("comments", new MongoDBList())
        put("loc", new DBGeoPoint(event.x, event.y))
        put("tags", event.tags)
        put("distance", event.distance)
        put("pace", event.pace)
    }

    private class DBEventComment(user: User, msg: String) extends BasicDBObject {
        put("msg", msg)
        put("date", new Date().getTime)
        put("user_id", user.id)
        put("user_name", user.first_name + " " + user.last_name)
        put("photo_url", user.photo_url)
    }

    //================================================================================================================//
}

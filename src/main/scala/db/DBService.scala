package db

import java.util.Date

import com.mongodb._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import model.{Event, User}

class DBService {

    val collection = getCollection()

    def saveEvent(user: User, event: Event) {
        collection.insert(new DBEvent(user, event))
    }

    def findEvents(user_id: String): DBCursor = {
        return collection.find(MongoDBObject("user.id" -> user_id))
    }

    def findEvents(x: Double, y: Double, max: Long, tags: Array[String]): DBCursor = {
        val point = MongoDBObject("type" -> "Point", "coordinates" -> GeoCoords(x, y))
        val geo = MongoDBObject("$geometry" -> point, "$maxDistance" -> max)
        val query = MongoDBObject("loc" -> MongoDBObject("$near" -> geo))
        if (tags.length > 0) query.put("tags", MongoDBObject("$elemMatch" -> MongoDBObject("$in" -> tags)))
        return collection.find(query).limit(50)
    }

    def removeParticipant(event_id: String, user: User): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> user.id)
        val participant = MongoDBObject("participants" -> MongoDBObject("id" -> user.id))
        val update = MongoDBObject("$pull" -> participant, "$inc" -> MongoDBObject("spots" -> 1))
        val doc = collection.findAndModify(event, null, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserNotPresent
    }

    def addParticipant(event_id: String, user: User): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val constraint = MongoDBObject("$nin" -> (MongoDBList.newBuilder[String] += user.id).result())
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> constraint)
        val participant = MongoDBObject("participants" -> new DBUser(user))
        val update = MongoDBObject("$addToSet" -> participant, "$inc" -> MongoDBObject("spots" -> -1))
        val doc = collection.findAndModify(event, null, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserAlreadyAdded
    }

    def addComment(event_id: String, user: User, msg: String): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> user.id)
        val comment = MongoDBObject("user_id" -> user.id, "msg" -> msg, "date" -> now)
        val update = MongoDBObject("$push" -> MongoDBObject("comments" -> comment))
        val doc = collection.findAndModify(event, null, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserNotPresent
    }

    def toJson(results: DBCursor): String = {
        return com.mongodb.util.JSON.serialize(results)
    }

    def toJson(result: DBObject): String = {
        return com.mongodb.util.JSON.serialize(result)
    }

    private def isEvent(id: String): Boolean = {
        return if (collection.find(MongoDBObject("_id" -> id)).limit(1).size() == 0) false else true
    }

    private def now(): Long = new Date().getTime

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }
}

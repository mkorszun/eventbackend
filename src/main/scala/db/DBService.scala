package db

import com.mongodb._
import com.mongodb.casbah.commons.MongoDBList
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.stormpath.sdk.directory.CustomData
import model.{Event, User}

class DBService {

    val collection = getCollection()

    def uuid = java.util.UUID.randomUUID.toString

    def saveEvent(user: User, event: Event) {
        val doc = new BasicDBObject()
        doc.put("_id", uuid)
        doc.put("user", userToObject(user))
        doc.put("spots", event.spots)
        doc.put("date_and_time", event.date_and_time)
        doc.put("headline", event.headline)
        doc.put("cost", event.cost)
        doc.put("duration", event.duration)
        doc.put("description", event.description)
        doc.put("participants", new MongoDBList())
        doc.put("loc", point(event.x, event.y))
        doc.put("tags", event.tags)
        collection.insert(doc)
    }

    def addParticipant(id: String, user: User): DBObject = {
        val constraint = () => {
            val nin = new BasicDBObject()
            val builder = MongoDBList.newBuilder[String] += user.id
            nin.put("$nin", builder.result())
            nin
        }
        val event: DBObject = updateParticipant(id, user, "$addToSet", -1, constraint)
        if (event != null) {
            return event
        }
        throw new UserAlreadyAdded
    }

    def removeParticipant(id: String, user: User): DBObject = {
        val event: DBObject = updateParticipant(id, user, "$pull", 1, () => user.id)
        if (event != null) {
            return event
        }
        throw new UserNotPresent
    }

    def findEvents(x: Double, y: Double, max: Long): DBCursor = {
        val near = new BasicDBObject()
        val loc = new BasicDBObject()
        val query = new BasicDBObject()

        near.put("$geometry", point(x, y))
        near.put("$maxDistance", max)

        loc.put("$near", near)
        query.put("loc", loc)
        return collection.find(query).limit(50)
    }

    def toJson(results: DBCursor): String = {
        return com.mongodb.util.JSON.serialize(results)
    }

    def toJson(result: DBObject): String = {
        return com.mongodb.util.JSON.serialize(result)
    }

    private def point(x: Double, y: Double): BasicDBObject = {
        val point = new BasicDBObject()
        point.put("type", "Point")
        point.put("coordinates", Array(x, y))
        return point
    }

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }

    private def userToObject(user: User): BasicDBObject = {
        val userDoc = new BasicDBObject()
        val data: CustomData = user.account.getCustomData
        userDoc.put("id", user.id)
        userDoc.put("photo_url", data.get("photo_url"))
        userDoc.put("age", data.get("age"))
        userDoc.put("bio", data.get("bio"))
        return userDoc
    }

    private def updateParticipant(id: String, user: User, ops: String, inc_val: Int,
        constraint: () => Object): DBObject = {

        if (!isEvent(id)) {
            throw new EventNotFound
        }

        val event = new BasicDBObject()
        event.put("_id", id.toString)
        event.put("participants.id", constraint())

        val participants = new BasicDBObject()
        participants.put("participants", userToObject(user))

        val update = new BasicDBObject()
        update.put(ops, participants)

        val inc = new BasicDBObject()
        inc.put("spots", inc_val)

        update.put("$inc", inc)
        return collection.findAndModify(event, null, null, false, update, true, false)
    }

    private def isEvent(id: String): Boolean = {
        val query = new BasicDBObject()
        query.put("_id", id)
        val cursor = collection.find(query).limit(1)
        if (cursor.size() == 0) {
            return false;
        } else {
            return true;
        }
    }
}

class EventNotFound extends Exception

class UserAlreadyAdded extends Exception

class UserNotPresent extends Exception

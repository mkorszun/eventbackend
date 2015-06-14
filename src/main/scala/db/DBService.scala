package db

import java.util.Date

import com.mongodb._
import com.mongodb.casbah.commons.MongoDBList
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import model.{Event, User}

class DBService {

    val collection = getCollection()

    def saveEvent(user: User, event: Event) {
        collection.insert(new DBEvent(user, event))
    }

    def addParticipant(id: String, user: User): DBObject = {
        val constraint = () => {
            val nin = new BasicDBObject()
            val builder = MongoDBList.newBuilder[String] += user.id
            nin.put("$nin", builder.result())
            nin
        }
        val event: DBObject = updateParticipant(id, user, "$addToSet", -1, constraint,
            new DBUser(user))
        if (event != null) {
            return event
        }
        throw new UserAlreadyAdded
    }

    def removeParticipant(id: String, user: User): DBObject = {
        val obj = new BasicDBObject()
        obj.put("id", user.id)

        val event: DBObject = updateParticipant(id, user, "$pull", 1, () => user.id, obj)
        if (event != null) {
            return event
        }
        throw new UserNotPresent
    }

    def addComment(id: String, user: User, msg: String): DBObject = {
        val event: DBObject = updateComments(id, user, msg, "$push", () => user.id)
        if (event != null) {
            return event
        }
        throw new UserNotPresent
    }

    def findEvents(x: Double, y: Double, max: Long, tags: Array[String]): DBCursor = {
        val near = new BasicDBObject()
        val loc = new BasicDBObject()
        val query = new BasicDBObject()

        near.put("$geometry", new DBGeoPoint(x, y))
        near.put("$maxDistance", max)

        loc.put("$near", near)
        query.put("loc", loc)

        if (tags.length > 0) {
            val in = new BasicDBObject()
            val m = new BasicDBObject()

            in.put("$in", tags)
            m.put("$elemMatch", in)
            query.put("tags", m)
        }

        return collection.find(query).limit(50)
    }

    def findUserEvents(id: String): DBCursor = {
        val query = new BasicDBObject()
        query.put("user.id", id)
        return collection.find(query)
    }

    def toJson(results: DBCursor): String = {
        return com.mongodb.util.JSON.serialize(results)
    }

    def toJson(result: DBObject): String = {
        return com.mongodb.util.JSON.serialize(result)
    }

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }

    private def updateParticipant(id: String, user: User, ops: String, inc_val: Int,
        constraint: () => Object, obj: BasicDBObject): DBObject = {

        if (!isEvent(id)) {
            throw new EventNotFound
        }

        val event = new BasicDBObject()
        event.put("_id", id.toString)
        event.put("participants.id", constraint())

        val participants = new BasicDBObject()
        participants.put("participants", obj)

        val update = new BasicDBObject()
        update.put(ops, participants)

        val inc = new BasicDBObject()
        inc.put("spots", inc_val)

        update.put("$inc", inc)
        return collection.findAndModify(event, null, null, false, update, true, false)
    }

    private def updateComments(id: String, user: User, msg: String, ops: String,
        constraint: () => Object): DBObject = {

        if (!isEvent(id)) {
            throw new EventNotFound
        }

        val event = new BasicDBObject()
        event.put("_id", id.toString)
        event.put("participants.id", constraint())

        val comment = new BasicDBObject()
        comment.put("user_id", user.id)
        comment.put("msg", msg)
        comment.put("date", new Date().getTime)

        val comments = new BasicDBObject()
        comments.put("comments", comment)

        val update = new BasicDBObject()
        update.put(ops, comments)

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

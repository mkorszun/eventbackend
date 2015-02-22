package db

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{BasicDBObject, DBCollection, DBCursor}
import model.Event

class DBService {

    val collection = getCollection()

    def saveEvent(event: Event) {
        val doc = new BasicDBObject()
        doc.put("user_id", event.user_id)
        doc.put("headline", event.headline)
        doc.put("cost", event.cost)
        doc.put("duration", event.duration)
        doc.put("description", event.description)
        doc.put("loc", point(event.x, event.y))
        collection.insert(doc)
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
}

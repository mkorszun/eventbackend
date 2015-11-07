package service.storage.utils

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{Cursor, DBCollection}

trait Storage {

    val EVENT_DETAILS_FIELDS = MongoDBObject("participants.devices" -> 0)
    val EVENT_LIST_FIELDS = MongoDBObject("comments" -> 0, "participants" -> 0, "deleted" -> 0)
    val EVENT_COMMENTS_FIELDS = MongoDBObject("comments" -> 1, "_id" -> 0)

    def getCollection(collection: String): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection(collection)
    }

    def aggregationSteps(ops: Array[MongoDBObject]): java.util.List[DBObject] = {
        val steps: java.util.List[DBObject] = new java.util.ArrayList[DBObject]()
        for (step <- ops) steps.add(step)
        return steps
    }

    def toArray(results: Cursor): Array[String] = {
        var res: Array[String] = Array()

        if (results.hasNext) {
            val value: AnyRef = results.next().get("aggregated")
            res = toArray(value.asInstanceOf[BasicDBList])
        }

        results.close()
        return res
    }

    def toArray(obj: BasicDBList): Array[String] = (obj.toList map (_.toString)).toArray

    class Group(field: String) extends BasicDBObject {
        put("_id", "all")
        put("aggregated", MongoDBObject("$addToSet" -> field))
    }

    class DBGeoPoint(x: Double, y: Double) extends BasicDBObject {
        put("type", "Point")
        put("coordinates", GeoCoords(y, x))
    }

}

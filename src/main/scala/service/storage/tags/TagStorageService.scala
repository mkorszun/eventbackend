package service.storage.tags

import com.mongodb
import com.mongodb._
import com.mongodb.casbah.Imports.{AggregationOptions, _}
import com.mongodb.casbah.commons.{Imports, MongoDBObject}
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI};

object TagStorageService {

    val collection = getCollection()

    // Public API ====================================================================================================//

    def allTags(): Array[String] = {
        val steps: java.util.List[mongodb.DBObject] = new java.util.ArrayList[mongodb.DBObject]()
        steps.add(MongoDBObject("$unwind" -> "$tags"))

        val group: Imports.DBObject = MongoDBObject("_id" -> "all", "popular" -> MongoDBObject("$addToSet" -> "$tags"))
        steps.add(MongoDBObject("$group" -> group))

        val results: Cursor = collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR))

        if (results.hasNext) {
            return (results.next().get("popular").asInstanceOf[BasicDBList].toList map (_.toString)).toArray
        } else {
            return Array()
        }
    }

    // Helpers =======================================================================================================//

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }

    // DB document objects ===========================================================================================//

    private class DBGeoPoint(x: Double, y: Double) extends MongoDBObject {
        put("type", "Point")
        put("coordinates", GeoCoords(x, y))
    }

    private object Exclusion extends BasicDBObject {
        put("_id", 0)
        put("tags", 1)
    }

    //================================================================================================================//
}

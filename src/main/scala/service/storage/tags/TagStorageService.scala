package service.storage.tags

import java.util.Calendar

import com.mongodb
import com.mongodb._
import com.mongodb.casbah.Imports.{AggregationOptions, _}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI};

object TagStorageService {

    val collection = getCollection()

    // Public API ====================================================================================================//

    def findTags(x: Double, y: Double, max: Long): Array[String] = {
        val timestamp = MongoDBObject("$gte" -> Calendar.getInstance().getTime().getTime)
        val steps: java.util.List[mongodb.DBObject] = new java.util.ArrayList[mongodb.DBObject]()

        steps.add(MongoDBObject("$geoNear" -> new DBGeoNear(x, y, max, timestamp)))
        steps.add(MongoDBObject("$unwind" -> "$tags"))
        steps.add(MongoDBObject("$group" -> new Group()))

        return result(collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR)))
    }

    // Helpers =======================================================================================================//

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }

    private def result(results: Cursor): Array[String] = {
        var res: Array[String] = Array()

        if (results.hasNext) {
            val value: AnyRef = results.next().get("aggregated")
            res = toArray(value.asInstanceOf[BasicDBList])
        }

        results.close()
        return res
    }

    private def toArray(obj: BasicDBList): Array[String] = (obj.toList map (_.toString)).toArray

    // DB document objects ===========================================================================================//

    private class DBGeoPoint(x: Double, y: Double) extends BasicDBObject {
        put("type", "Point")
        put("coordinates", GeoCoords(x, y))
    }

    private class DBGeoNear(x: Double, y: Double, max: Long, timestamp: MongoDBObject) extends BasicDBObject {
        put("near", new DBGeoPoint(x, y))
        put("maxDistance", max)
        put("query", MongoDBObject("timestamp" -> timestamp))
        put("spherical", true)
        put("distanceField", "dist.location")
    }

    private class Group extends BasicDBObject {
        put("_id", "all")
        put("aggregated", MongoDBObject("$addToSet" -> "$tags"))
    }

    //================================================================================================================//
}

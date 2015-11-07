package service.storage.tags

import java.util.Calendar

import com.mongodb
import com.mongodb.casbah.Imports.{AggregationOptions, _}
import com.mongodb.casbah.commons.MongoDBObject
import service.storage.utils.Storage

object TagStorageService extends Storage {

    val collection = getCollection("events")

    // Public API ====================================================================================================//

    def findTags(x: Option[Double], y: Option[Double], max: Option[Long]): Array[String] = {

        val steps: java.util.List[mongodb.DBObject] = new java.util.ArrayList[mongodb.DBObject]()

        if (x.isDefined && y.isDefined && max.isDefined) {
            val timestamp = MongoDBObject("$gte" -> Calendar.getInstance().getTime().getTime)
            steps.add(MongoDBObject("$geoNear" -> new DBGeoNear(x.get, y.get, max.get, timestamp)))
        }

        steps.add(MongoDBObject("$unwind" -> "$tags"))
        steps.add(MongoDBObject("$group" -> new Group("$tags", "all")))

        return toArray(collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR)))
    }

    // DB document objects ===========================================================================================//

    private class DBGeoNear(x: Double, y: Double, max: Long, timestamp: MongoDBObject) extends BasicDBObject {
        put("near", new DBGeoPoint(x, y))
        put("maxDistance", max)
        put("query", MongoDBObject("timestamp" -> timestamp))
        put("spherical", true)
        put("distanceField", "dist.location")
    }

    //================================================================================================================//
}

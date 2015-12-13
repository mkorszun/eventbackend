package service.storage.utils

import java.util.Date

import com.mongodb
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.dsl.GeoCoords
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{Cursor, DBCollection}

trait Storage {

    val UPDATED_AT = "updated_at"
    val CREATED_AT = "created_at"
    val COMMENTS_COUNT = "comments_count";
    val EVENT_DETAILS_FIELDS = MongoDBObject("participants.devices" -> 0)
    val EVENT_DETAILS_ADMIN_FIELDS = MongoDBObject("participants" -> 0, "comments" -> 0)
    val EVENT_LIST_FIELDS = MongoDBObject("comments" -> 0, "participants" -> 0, "deleted" -> 0)
    val EVENT_LIST_TIMESTAMPS = MongoDBObject("timestamp" -> 1, UPDATED_AT -> 1, COMMENTS_COUNT -> 1)
    val EVENT_LIST_ADMIN_FIELDS = MongoDBObject("_id" -> 1, "headline" -> 1, "timestamp" -> 1, "distance" -> 1)
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

    def aggregate(collection: DBCollection, steps: java.util.List[DBObject]): Cursor = {
        collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR))
    }

    def toArray(results: Cursor): Array[String] = {
        return toArray(results, "_id").res1
    }

    def toArray(results: Cursor, field: String): GroupResult = {
        var res1: Array[String] = Array()
        var res2: String = ""

        if (results.hasNext) {
            val next: mongodb.DBObject = results.next()
            val value: AnyRef = next.get("aggregated")
            res1 = toArray(value.asInstanceOf[BasicDBList])
            res2 = next.getAs[String]("_id").get
        }

        results.close()
        return GroupResult(res1, res2)
    }

    def updated: MongoDBObject = MongoDBObject(UPDATED_AT -> new Date().getTime)

    def toArray(obj: BasicDBList): Array[String] = (obj.toList map (_.toString)).toArray

    class Group(field: String, field1: String) extends BasicDBObject {
        put("_id", field1)
        put("aggregated", MongoDBObject("$addToSet" -> field))
    }

    class DBGeoPoint(x: Double, y: Double) extends BasicDBObject {
        put("type", "Point")
        put("coordinates", GeoCoords(y, x))
    }

    case class GroupResult(res1: Array[String], res2: String) {
        override def toString: String = res1.mkString(",")
    }

}

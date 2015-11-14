package worker

import com.mongodb.DBCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{Imports, MongoClient, MongoClientURI, MongoDB}

object DBInitWorker {

    val collections: Array[String] = Array("events", "users")

    val event_indexes: Array[Imports.DBObject] = Array(
        MongoDBObject("participants.id" -> 1, "timestamp" -> 1),
        MongoDBObject("loc" -> "2dsphere", "timestamp" -> 1, "tags" -> 1),
        MongoDBObject("participants.id" -> 1, "user.id" -> 1),
        MongoDBObject("_id" -> 1, "user.id" -> 1),
        MongoDBObject("loc" -> "2dsphere"),
        MongoDBObject("user.id" -> 1),
        MongoDBObject("participants.id" -> 1),
        MongoDBObject("tags" -> 1),
        MongoDBObject("timestamp" -> 1)

    )

    val user_indexes: Array[Imports.DBObject] = Array(
        MongoDBObject("provider" -> 1, "provider_id" -> 1),
        MongoDBObject("_id" -> 1, "token" -> 1)
    )
    val db = getDB()

    def getDB(): MongoDB = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db
    }

    def init(): Unit = {
        println("Creating collections")
        val users: DBCollection = db.getCollection("users")
        val events: DBCollection = db.getCollection("events")

        println("Creating indexes")
        for (collection <- collections) {
            if (!db.collectionExists(collection)) db.createCollection(collection, MongoDBObject())
        }
        for (idx <- user_indexes) users.createIndex(idx)
        for (idx <- event_indexes) events.createIndex(idx)
    }

    def main(args: Array[String]): Unit = {
        init()
    }
}

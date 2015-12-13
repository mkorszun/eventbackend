package worker

import com.mongodb.DBCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{Imports, MongoClient, MongoClientURI, MongoDB}

object DBInitWorker {

    val UNIQUE = MongoDBObject("unique" -> true)
    val collections: Array[String] = Array("events", "users", "password_reset_tokens")

    val event_indexes: Array[Imports.DBObject] = Array(
        MongoDBObject("participants.id" -> 1, "timestamp" -> 1),
        MongoDBObject("loc" -> "2dsphere", "timestamp" -> 1, "tags" -> 1),
        MongoDBObject("participants.id" -> 1, "_id" -> 1),
        MongoDBObject("_id" -> 1, "user.id" -> 1)
    )

    val user_indexes: Array[Imports.DBObject] = Array(
        MongoDBObject("provider" -> 1, "provider_id" -> 1),
        MongoDBObject("_id" -> 1, "token" -> 1),
        MongoDBObject("email" -> 1, "verified" -> 1),
        MongoDBObject("token" -> 1, "verified" -> 1)
    )

    val unique_user_indexes: Array[Imports.DBObject] = Array(
        MongoDBObject("email" -> 1)
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
        val password_reset_tokens = db.getCollection("password_reset_tokens")

        for (collection <- collections) {
            if (!db.collectionExists(collection)) db.createCollection(collection, MongoDBObject())
        }

        println("Creating indexes")
        for (idx <- user_indexes) users.createIndex(idx)
        for (idx <- event_indexes) events.createIndex(idx)
        for (idx <- unique_user_indexes) users.createIndex(idx, UNIQUE)

        users.createIndex(MongoDBObject("unverified_since" -> 1), MongoDBObject("expireAfterSeconds" -> 1440))
        password_reset_tokens.createIndex(MongoDBObject("created_at" -> 1), MongoDBObject("expireAfterSeconds" -> 3600))
    }

    def main(args: Array[String]): Unit = {
        init()
    }
}

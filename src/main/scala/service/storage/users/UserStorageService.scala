package service.storage.users

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBObject}
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{DBObject, _}
import model.token.Token
import model.user.{PublicUser, User}

object UserStorageService {

    val collection = getCollection()

    // Public API ====================================================================================================//

    def createUser(user: User): Token = {
        val query = MongoDBObject("provider_id" -> user.provider_id, "provider" -> user.provider)
        val setOnInsert = MongoDBObject("$setOnInsert" -> userToDocument(user))
        val doc = collection.findAndModify(query, null, null, false, setOnInsert, true, true)
        return tokenFromDocument(doc)
    }

    def updateUser(id: String, token: String, user: PublicUser): Option[PublicUser] = {

        val update = $set(
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "bio" -> user.bio,
            "tags" -> user.tags
        )

        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(publicUserFromDocument(doc)) else None
    }

    def readPrivateUserData(token: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject("token" -> token))
        if (doc != null) Option(userFromDocument(doc)) else None
    }

    def readPublicUserData(id: String): Option[PublicUser] = {
        val doc = collection.findOne(MongoDBObject("_id" -> id))
        if (doc != null) Option(publicUserFromDocument(doc)) else None
    }

    // Helpers =======================================================================================================//

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("users")
    }

    // DB document objects ===========================================================================================//

    def userToDocument(user: User): DBObject = {
        MongoDBObject(
            "_id" -> user.id,
            "provider_id" -> user.provider_id,
            "provider" -> user.provider,
            "token" -> user.token,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "tags" -> user.tags
        )
    }

    def userToPublicDocument(user: User): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "tags" -> user.tags
        )
    }

    def userFromDocument(doc: DBObject): User = {
        User(
            doc.get("_id").toString,
            doc.get("provider_id").toString,
            doc.get("provider").toString,
            doc.get("token").toString,
            doc.get("first_name").toString,
            doc.get("last_name").toString,
            doc.get("photo_url").toString,
            doc.get("bio").toString,
            (doc.get("tags").asInstanceOf[BasicDBList].toList map (_.toString)).toArray
        )
    }

    def publicUserFromDocument(doc: DBObject): PublicUser = {
        PublicUser(
            doc.get("first_name").toString,
            doc.get("last_name").toString,
            doc.get("photo_url").toString,
            doc.get("bio").toString,
            (doc.get("tags").asInstanceOf[BasicDBList].toList map (_.toString)).toArray
        )
    }

    def tokenFromDocument(doc: DBObject): Token = {
        Token(
            doc.get("_id").toString,
            doc.get("token").toString
        )
    }

    //================================================================================================================//
}

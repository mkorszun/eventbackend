package service.storage.users

import java.util.concurrent.Executors

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBObject}
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{DBObject, _}
import model.token.Token
import model.user.{PublicUser, User, UserDevice}
import service.storage.events.EventStorageService

import scala.concurrent.{ExecutionContext, Future}

object UserStorageService {

    val collection = getCollection()
    val executorService = Executors.newFixedThreadPool(10)
    val eventStorageService = new EventStorageService
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

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
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
        )

        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(publicUserFromDocument(doc)) else None
    }

    def updatePhoto(id: String, token: String, photo_url: String): Option[PublicUser] = {
        val update = $set("photo_url" -> photo_url)
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

    def updateUserData(id: String, user: PublicUser) = Future {
        eventStorageService.updateOwnerData(id, user)
        eventStorageService.updateParticipantsData(id, user)
    }

    def updateUserDevice(id: String, device: UserDevice): Unit = {
        val query = MongoDBObject("_id" -> id)
        val deviceDoc = MongoDBObject("device_token" -> device.device_token, "platform" -> device.platform)
        val update = MongoDBObject("$addToSet" -> MongoDBObject("devices" -> deviceDoc))
        collection.findAndModify(query, null, null, false, update, true, false)
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
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
        )
    }

    def userToPublicDocument(user: User): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
        )
    }

    def userToParticipantDocument(user: User): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "photo_url" -> user.photo_url
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
            Option(doc.get("telephone").asInstanceOf[String]),
            Option(doc.get("www").asInstanceOf[String]),
            Option(doc.get("email").asInstanceOf[String])
        )
    }

    def publicUserFromDocument(doc: DBObject): PublicUser = {
        PublicUser(
            Option(doc.get("_id").toString),
            doc.get("first_name").toString,
            doc.get("last_name").toString,
            doc.get("photo_url").toString,
            doc.get("bio").toString,
            Option(doc.get("telephone").asInstanceOf[String]),
            Option(doc.get("www").asInstanceOf[String]),
            Option(doc.get("email").asInstanceOf[String])
        )
    }

    def publicUserToDocument(user: PublicUser): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
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

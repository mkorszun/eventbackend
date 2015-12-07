package service.storage.auth

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import model.token.Token
import model.user.User
import service.storage.utils.Storage

object AuthStorageService extends Storage {

    val collection = getCollection("users")

    val ID = "_id"
    val EMAIL = "email"
    val PROVIDER_ID = "provider_id"
    val PROVIDER = "provider"
    val TOKEN = "token"
    val VERIFIED = "verified"
    val CONFIRMATION_TOKEN = "confirmation_token"
    val UNVERIFIED_SINCE = "unverified_since"

    def createUser(user: User): User = {
        collection.insert(User.toDocument(user))
        return user
    }

    def getOrCreate(user: User): Token = {
        val query = MongoDBObject(PROVIDER_ID -> user.provider_id, PROVIDER -> user.provider)
        val setOnInsert = MongoDBObject("$setOnInsert" -> User.toDocument(user))
        val doc = collection.findAndModify(query, null, null, false, setOnInsert, true, true)
        return Token.fromDocument(doc)
    }

    def loadUserByEmail(email: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject(EMAIL -> email, VERIFIED -> true))
        if (doc != null) Option(User.fromDocument(doc)) else None
    }

    def loadUserByToken(token: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject(TOKEN -> token, VERIFIED -> true))
        if (doc != null) Option(User.fromDocument(doc)) else None
    }

    def confirm(id: String, token: String): Unit = {
        val query = MongoDBObject(CONFIRMATION_TOKEN -> token, ID -> id)
        val update = $set(VERIFIED -> true) ++ $unset(UNVERIFIED_SINCE, CONFIRMATION_TOKEN)
        val doc = collection.findAndModify(query, MongoDBObject(), null, false, update, true, false)
        if (doc == null) throw new UserExpiredException
    }
}

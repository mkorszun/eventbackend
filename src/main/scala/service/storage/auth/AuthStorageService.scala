package service.storage.auth

import com.mongodb.casbah.commons.MongoDBObject
import model.token.Token
import model.user.User
import service.storage.utils.Storage

object AuthStorageService extends Storage {

    val collection = getCollection("users")

    val EMAIL = "email"
    val PROVIDER_ID = "provider_id"
    val PROVIDER = "provider"
    val TOKEN = "token"

    def createUser(user: User): Token = {
        collection.insert(User.toDocument(user))
        return Token.fromUser(user)
    }

    def getOrCreate(user: User): Token = {
        val query = MongoDBObject(PROVIDER_ID -> user.provider_id, PROVIDER -> user.provider)
        val setOnInsert = MongoDBObject("$setOnInsert" -> User.toDocument(user))
        val doc = collection.findAndModify(query, null, null, false, setOnInsert, true, true)
        return Token.fromDocument(doc)
    }

    def loadUserByEmail(email: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject(EMAIL -> email))
        if (doc != null) Option(User.fromDocument(doc)) else None
    }

    def loadUserByToken(token: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject(TOKEN -> token))
        if (doc != null) Option(User.fromDocument(doc)) else None
    }
}

package service.storage.auth

import java.util.Date

import auth.BearerTokenGenerator
import com.mongodb.{CommandFailureException, DuplicateKeyException}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import model.token.Token
import model.user.User
import service.storage.utils.Storage

object AuthStorageService extends Storage {

    val collection = getCollection("users")
    val password_reset_tokens = getCollection("password_reset_tokens")

    val ID = "_id"
    val EMAIL = "email"
    val PROVIDER_ID = "provider_id"
    val PROVIDER = "provider"
    val TOKEN = "token"
    val VERIFIED = "verified"
    val CONFIRMATION_TOKEN = "confirmation_token"
    val UNVERIFIED_SINCE = "unverified_since"
    val PASSWORD_RESET_TOKEN = "token"
    val PASSWORD = "password"

    def createUser(user: User): User = {
        try {
            collection.insert(User.toDocument(user))
            return user
        } catch {
            case e: DuplicateKeyException =>
                throw new EmailAlreadyExists
        }
    }

    def getOrCreate(user: User): Token = {
        try {
            val query = MongoDBObject(PROVIDER_ID -> user.provider_id, PROVIDER -> user.provider)
            val setOnInsert = MongoDBObject("$setOnInsert" -> User.toDocument(user))
            val doc = collection.findAndModify(query, null, null, false, setOnInsert, true, true)
            return Token.fromDocument(doc)
        } catch {
            case _: DuplicateKeyException | _: CommandFailureException =>
                throw new EmailAlreadyExists
        }
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

    def createPasswordResetToken(id: String): String = {
        val token = Option(BearerTokenGenerator.generateSHAToken(id))
        val doc = MongoDBObject(PASSWORD_RESET_TOKEN -> token, CREATED_AT -> new Date())
        password_reset_tokens.update(MongoDBObject(ID -> id), doc, true, false)
        return token.get
    }

    def resetPassword(id: String, token: String, password: String): Unit = {
        val doc = password_reset_tokens.findOne(MongoDBObject(ID -> id, PASSWORD_RESET_TOKEN -> token))
        if (doc == null) throw new InvalidResetTokenException
        collection.update(MongoDBObject(ID -> id), $set(PASSWORD -> password))
    }
}

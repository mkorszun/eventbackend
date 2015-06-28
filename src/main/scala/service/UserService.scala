package service

import akka.actor.Actor
import com.stormpath.sdk.account.Account
import com.stormpath.sdk.api.{ApiKey, ApiKeys}
import com.stormpath.sdk.application.{Application, ApplicationList, Applications}
import com.stormpath.sdk.client.{Client, Clients}
import com.stormpath.sdk.directory.CustomData
import com.stormpath.sdk.ds.DataStore
import com.stormpath.sdk.provider.{FacebookAccountRequestBuilder, ProviderAccountRequest, Providers}
import com.stormpath.sdk.tenant.Tenant
import model.{User, UserData}

case class GetUserByToken(token: String)

case class GetUserById(id: String)

case class UpdateUserData(user: User, user_data: UserData)

class UserService extends Actor {

    override def receive: Receive = {
        case GetUserByToken(token) =>
            try {
                val account: Account = getAccount(token)
                sender ! Some(User(account))
            } catch {
                case e: Exception =>
                    sender ! None
            }
        case GetUserById(id) =>
            try {
                val href: String = "https://api.stormpath.com/v1/accounts/" + id
                val account: Account = AuthClient.getClient().getResource(href, classOf[Account])
                sender ! Some(User(account))
            } catch {
                case e: Exception =>
                    sender ! None
            }
        case UpdateUserData(user, user_data) =>
            try {
                val customData: CustomData = user.account.getCustomData
                customData.put("photo_url", user_data.photo_url)
                customData.put("age", user_data.age.toString)
                customData.put("bio", user_data.bio.toString)
                customData.put("tags", user_data.tags)
                customData.put("first_name", user_data.firstName)
                customData.put("last_name", user_data.lastName)
                user.account.save()
                sender ! Some(user)
            } catch {
                case e: Exception =>
                    sender ! None
            }
        case _ => sender ! None
    }

    private def getAccount(token: String): Account = {
        val requestBuilder: FacebookAccountRequestBuilder = Providers.FACEBOOK.account()
        val accountRequest: ProviderAccountRequest = requestBuilder.setAccessToken(token).build()
        return AuthApplication.auth_app.getAccount(accountRequest).getAccount
    }

    private object AuthClient {

        val client: Client = getClient()
        val dataStore: DataStore = client.getDataStore

        def getClient(): Client = {
            val id: String = System.getenv("STORMPATH_ID")
            val secret: String = System.getenv("STORMPATH_SECRET")
            val apiKey: ApiKey = ApiKeys.builder().setSecret(secret).setId(id).build()
            return Clients.builder().setApiKey(apiKey).build()
        }
    }

    private object AuthApplication {

        val auth_app: Application = getApplication()

        def getApplication(): Application = {
            val client: Client = AuthClient.client
            val app_name: String = System.getenv("STORMPATH_APP_NAME")
            val tenant: Tenant = client.getCurrentTenant()
            val applications: ApplicationList = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase(app_name))
            )

            return applications.iterator().next()
        }
    }

}
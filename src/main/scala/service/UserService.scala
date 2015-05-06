package service

import akka.actor.Actor
import com.stormpath.sdk.account.Account
import com.stormpath.sdk.api.{ApiKey, ApiKeys}
import com.stormpath.sdk.application.{Application, ApplicationList, Applications}
import com.stormpath.sdk.client.{Client, Clients}
import com.stormpath.sdk.provider.{FacebookAccountRequestBuilder, ProviderAccountRequest, Providers}
import com.stormpath.sdk.tenant.Tenant
import model.User

case class GetUser(token: String)

class UserService extends Actor {

    override def receive: Receive = {
        case GetUser(token) =>
            try {
                val account: Account = getAccount(token)
                val email: String = account.getEmail
                val id: String = getId(account)
                sender ! Some(User(id, email))
            } catch {
                case e: Exception =>
                    sender ! None
            }
        case _ => sender ! None
    }

    def getAccount(token: String): Account = {
        val requestBuilder: FacebookAccountRequestBuilder = Providers.FACEBOOK.account()
        val accountRequest: ProviderAccountRequest = requestBuilder.setAccessToken(token).build()
        return AuthApplication.auth_app.getAccount(accountRequest).getAccount
    }

    def getId(account: Account): String = {
        val uri: String = account.getHref()
        return uri.replaceFirst(".*/([^/?]+).*", "$1")
    }
}

object AuthApplication {

    val auth_app: Application = getApplication()

    def getApplication(): Application = {
        val id: String = System.getenv("STORMPATH_ID")
        val secret: String = System.getenv("STORMPATH_SECRET")
        val app_name: String = System.getenv("STORMPATH_APP_NAME")

        val apiKey: ApiKey = ApiKeys.builder().setSecret(secret).setId(id).build()
        val client: Client = Clients.builder().setApiKey(apiKey).build()

        val tenant: Tenant = client.getCurrentTenant()
        val applications: ApplicationList = tenant.getApplications(
            Applications.where(Applications.name().eqIgnoreCase(app_name))
        )

        return applications.iterator().next()
    }
}

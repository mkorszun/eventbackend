package model

import com.stormpath.sdk.account.Account

case class User(account: Account) {
    val id: String = account.getHref().replaceFirst(".*/([^/?]+).*", "$1")
}
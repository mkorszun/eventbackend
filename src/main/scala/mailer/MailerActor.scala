package mailer

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging}
import com.sendgrid.SendGrid
import config.Config

import scala.concurrent.{ExecutionContext, Future}

case class AccountConfirmation(id: String, token: String, email: String)

case class PasswordReset(id: String, token: String, email: String)

class MailerActor extends Actor with ActorLogging with Config {

    val executorService = Executors.newFixedThreadPool(MAILER_EXECUTOR_SIZE)
    val client = new SendGrid(SENDGRID_USERNAME, SENDGRID_PASSWORD)

    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case AccountConfirmation(id, token, email) =>
            Future {

                val confirmation_link = String.format(CONFIRMATION_LINK_BASE, id, token)
                val message: SendGrid.Email = new SendGrid.Email()

                log.info(f"Sending confirmation link $confirmation_link to $email")
                message.addTo(email)
                message.setFrom(MAILER_SENDER)
                message.setSubject(ACCOUNT_CONFIRMATION_TITLE)
                message.setText(confirmation_link)

                val response: SendGrid.Response = client.send(message)
                log.info("Mailer result " + response.getMessage)
            }
        case PasswordReset(id, token, email) =>
            Future {
                val reset_link = String.format(RESET_LINK_BASE, id, token)
                val message: SendGrid.Email = new SendGrid.Email()

                log.info(f"Sending reset password link $reset_link to $email")
                message.addTo(email)
                message.setFrom(MAILER_SENDER)
                message.setSubject(PASSWORD_RESET_TITLE)
                message.setText(reset_link)

                val response: SendGrid.Response = client.send(message)
                log.info("Mailer result " + response.getMessage)
            }
    }
}
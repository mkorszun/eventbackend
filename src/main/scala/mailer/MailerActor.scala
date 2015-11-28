package mailer

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging, Props}
import com.sendgrid.SendGrid
import config.Config
import push.DeviceRegistrationActor

import scala.concurrent.{ExecutionContext, Future}

case class AccountConfirmation(id: String, token: String, email: String)

class MailerActor extends Actor with ActorLogging with Config {

    val deviceActor = context.actorOf(Props[DeviceRegistrationActor])
    val executorService = Executors.newFixedThreadPool(MAILER_EXECUTOR_SIZE)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case AccountConfirmation(id, token, email) =>
            Future {
                val sendgrid: SendGrid = new SendGrid(SENDGRID_USERNAME, SENDGRID_PASSWORD)
                val confirmation_link = String.format(CONFIRMATION_LINK_BASE, id, token)
                val message: SendGrid.Email = new SendGrid.Email()

                log.info(f"Sending confirmation link $confirmation_link to $email")
                message.addTo(email)
                message.setFrom(MAILER_SENDER)
                message.setSubject(ACCOUNT_CONFIRMATION_TITLE)
                message.setText(confirmation_link)

                val response : SendGrid.Response = sendgrid.send(message)
                log.info("Sendgrid result " + response.getMessage)
            }
    }
}
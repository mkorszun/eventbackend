package mailer

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.model._
import config.Config

import scala.concurrent.{ExecutionContext, Future}

case class AccountConfirmation(id: String, token: String, email: String)

case class PasswordReset(id: String, token: String, email: String)

class MailerActor extends Actor with ActorLogging with Config {

    val executorService = Executors.newFixedThreadPool(MAILER_EXECUTOR_SIZE)
    val client: AmazonSimpleEmailServiceClient = new AmazonSimpleEmailServiceClient()
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    client.setRegion(Region.getRegion(Regions.EU_WEST_1))

    override def receive: Receive = {
        case AccountConfirmation(id, token, email) =>
            val f = Future {
                val confirmation_link = String.format(CONFIRMATION_LINK_BASE, id, token)
                log.info(f"Sending confirmation link $confirmation_link to $email")
                val body = String.format(CONFIRMATION_EMAIL, confirmation_link)
                sendEmail(email, ACCOUNT_CONFIRMATION_TITLE, body)
            }

            f onFailure {
                case e =>
                    log.error(e, f"Failed to send email")
            }
        case PasswordReset(id, token, email) =>
            val f = Future {
                val reset_link = String.format(RESET_LINK_BASE, id, token)
                log.info(f"Sending reset link $reset_link to $email")
                val body = String.format(PASSWORD_RESET_EMAIL, reset_link)
                sendEmail(email, PASSWORD_RESET_TITLE, body)
            }

            f onFailure {
                case e =>
                    log.error(e, f"Failed to send email")
            }
    }

    private def sendEmail(to: String, title: String, content: String): Unit = {
        val destination: Destination = new Destination().withToAddresses(to)
        val subject: Content = new Content().withData(title)
        val textBody: Content = new Content().withData(content)
        val body: Body = new Body().withHtml(textBody)

        val message: Message = new Message().withSubject(subject).withBody(body)
        val request: SendEmailRequest = new SendEmailRequest()
            .withSource(MAILER_SENDER)
            .withDestination(destination)
            .withMessage(message)

        val response = client.sendEmail(request)
        log.info("Mailer result " + response)
    }
}
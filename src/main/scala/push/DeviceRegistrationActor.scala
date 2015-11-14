package push

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging}
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import model.user.UserDevice
import service.aws.SNSClient
import service.storage.users.UserStorageService

import scala.concurrent.{ExecutionContext, Future}

case class RegisterDevice(user_id: String, device: UserDevice)

case class UnregisterDevice(user_id: String, arn: String)

class DeviceRegistrationActor extends Actor with ActorLogging with SNSClient {

    val executorService = Executors.newFixedThreadPool(REGISTRATION_EXECUTOR_SIZE)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    override def receive: Receive = {
        case RegisterDevice(user_id, device) =>
            val f = Future {
                log.info(f"Registering new device for user $user_id: $device")
                val request: CreatePlatformEndpointRequest = new CreatePlatformEndpointRequest()

                request.withPlatformApplicationArn(getARNForPlatform(device.platform))
                request.withToken(device.device_token)

                val arn = client.createPlatformEndpoint(request).getEndpointArn
                log.info(f"SNS platform endpoint created for user $user_id: $arn")
                UserStorageService.updateUserDevice(user_id, arn)
            }

            f onFailure {
                case e =>
                    log.error(e, f"Failed to register new device for user $user_id: $device")
            }
        case UnregisterDevice(user_id, arn) =>
            val f = Future {
                log.info(f"Removing device $arn for user $user_id")
                UserStorageService.removeDevice(arn)
            }

            f onFailure {
                case e =>
                    log.error(e, f"Failed to remove device $arn for user $user_id")
            }
    }
}

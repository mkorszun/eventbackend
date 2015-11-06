package push

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging}
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import model.user.UserDevice
import service.aws.SNSClient
import service.storage.events.EventStorageService
import service.storage.users.UserStorageService

import scala.concurrent.{ExecutionContext, Future}

case class RegisterDevice(user_id: String, device: UserDevice)

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

                val devices: Array[String] = UserStorageService.updateUserDevice(user_id, arn)
                EventStorageService.updateParticipantsDevices(user_id, devices)
                log.info(f"Updated all participations with new device list for user $user_id")
            }

            f onFailure {
                case e =>
                    log.error(e, f"Failed to register new device for user $user_id: $device")
            }
    }
}

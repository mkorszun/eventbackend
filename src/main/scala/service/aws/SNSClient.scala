package service.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.{EndpointDisabledException, PublishRequest}
import config.Config

trait SNSClient extends Config {
    val client: AmazonSNSClient = new AmazonSNSClient(new AWSCredentials {
        override def getAWSAccessKeyId: String = AWS_ACCESS_KEY_ID

        override def getAWSSecretKey: String = AWS_SECRET_ACCESS_KEY
    })

    client.setRegion(Region.getRegion(Regions.EU_WEST_1))

    def getARNForPlatform(platform: String): String = platform match {
        case "android" => APP_ANDROID_ARN
        case "ios" => APP_IOS_ARN
    }

    def push(arn: String, msg: String): Boolean = {
        try {
            client.publish(new PublishRequest().withTargetArn(arn).withMessage(msg))
            return true
        } catch {
            case e: EndpointDisabledException => return false
        }
    }
}

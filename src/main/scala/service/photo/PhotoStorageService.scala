package service.photo

import java.util.Calendar

import awscala.s3.{Bucket, S3}
import com.amazonaws.services.s3.model.ObjectMetadata
import config.Config

object PhotoStorageService extends Object with Config {
    implicit val region = awscala.Region.Ireland
    implicit val s3 = S3(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)

    val bucket: Bucket = s3.bucket(S3_BUCKET_NAME) match {
        case Some(photoBucket) => photoBucket
        case None => s3.createBucket(S3_BUCKET_NAME)
    }

    def upload(filename: String, bytes: Array[Byte]): String = {
        val metadata: ObjectMetadata = new ObjectMetadata()
        metadata.setContentType("image/jpeg")
        bucket.putObjectAsPublicRead(filename, bytes, metadata)
        Seq(S3_PREFIX, S3_BUCKET_NAME, filename).mkString("/")
    }

    def name(id: String): String = {
        id + "_" + Calendar.getInstance().getTime().getTime + ".jpg"
    }
}

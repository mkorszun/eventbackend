package config

trait Config {
    val MAX_AGE_SEARCH = System.getenv("MAX_AGE_SEARCH").toInt
    val MAX_AGE_USER_EVENTS = System.getenv("MAX_AGE_USER_EVENTS").toInt
    val MAX_AGE_TAGS = System.getenv("MAX_AGE_TAGS").toInt
    val DOCUMENTATION_URL = System.getenv("DOC_URL")
    val AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID")
    val AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY")
    val S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME")
    val S3_PREFIX = System.getenv("S3_PREFIX")
}

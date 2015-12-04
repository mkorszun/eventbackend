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
    val APP_ANDROID_ARN = System.getenv("APP_ANDROID_ARN")
    val APP_IOS_ARN = System.getenv("APP_IOS_ARN")
    val PUSH_EXECUTOR_SIZE = System.getenv("PUSH_EXECUTOR_SIZE").toInt
    val REGISTRATION_EXECUTOR_SIZE = System.getenv("REGISTRATION_EXECUTOR_SIZE").toInt
    val PUSH_MSG_NEW_COMMENT = sys.env.get("PUSH_MSG_NEW_COMMENT").getOrElse("%s dodał komentarz w Twojim treningu %s")
    val PUSH_MSG_NEW_PARTICIPANT = sys.env.get("PUSH_MSG_NEW_PARTICIPANT").getOrElse("%s dolączył do Twojego treningu %s")
    val PUSH_MSG_LEAVING_PARTICIPANT = sys.env.get("PUSH_MSG_LEAVING_PARTICIPANT").getOrElse("%s odszedł z Twojego treningu %s")
    val PUSH_MSG_UPDATED_EVENT = sys.env.get("PUSH_MSG_UPDATED_EVENT").getOrElse("%s zaktualizował trening %s")
    val CONFIRMATION_LINK_BASE = sys.env.get("CONFIRMATION_LINK_BASE").getOrElse("https://development-biegajmy.cloudcontrolled.com/user/%s/confirm?token=%s")
    val SENDGRID_USERNAME = sys.env.get("SENDGRID_USERNAME").getOrElse("depp6492dt8@cloudcontrolled.com")
    val SENDGRID_PASSWORD = sys.env.get("SENDGRID_PASSWORD").getOrElse("corcosa93336")
    val MAILER_SENDER = sys.env.get("MAILER_SENDER").getOrElse("noreply@biegaj.my")
    val MAILER_EXECUTOR_SIZE = System.getenv("REGISTRATION_EXECUTOR_SIZE").toInt
    val ACCOUNT_CONFIRMATION_TITLE = sys.env.get("ACCOUNT_CONFIRMATION_TITLE").getOrElse("Biegajmy - Link aktywacyjny")
    val CONFIRMATION_REDIRECT = sys.env.get("CONFIRMATION_REDIRECT").getOrElse("https://s3-eu-west-1.amazonaws.com/biegajmy/registration_confirmation/index.html")
}

package config

trait Config {
    val MAX_AGE_SEARCH = System.getenv("MAX_AGE_SEARCH").toInt
    val MAX_AGE_USER_EVENTS = System.getenv("MAX_AGE_USER_EVENTS").toInt
    val MAX_AGE_TAGS = System.getenv("MAX_AGE_TAGS").toInt
    val DOCUMENTATION_URL = System.getenv("DOC_URL")
}

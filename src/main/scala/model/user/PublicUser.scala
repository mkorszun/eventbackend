package model.user

case class PublicUser(
    first_name: String,
    last_name: String,
    photo_url: String,
    bio: String,
    tags: Array[String])

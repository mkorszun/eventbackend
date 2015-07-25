package model.user

case class User(
    id: String,
    provider_id: String,
    provider: String,
    token: String,
    first_name: String,
    last_name: String,
    photo_url: String,
    bio: String,
    tags: Array[String])

package model.user

case class PublicUser(
    id: Option[String],
    first_name: String,
    last_name: Option[String],
    photo_url: Option[String],
    bio: Option[String],
    telephone: Option[String],
    www: Option[String],
    email: Option[String])

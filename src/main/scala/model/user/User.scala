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
    telephone: Option[String],
    www: Option[String],
    email: Option[String],
    devices: Option[Array[String]]) {

    def fullName: String = f"$first_name $last_name"
}

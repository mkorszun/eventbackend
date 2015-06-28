package model

case class UserData(id: String, photo_url: String, age: Int, bio: String, tags: Array[String],
    firstName: String, lastName: String)

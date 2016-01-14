package model.admin

import model.user.PublicUser

case class AdminEvent(headline: String, description: Option[String], timestamp: Long, x: Double, y: Double,
    tags: Option[Array[String]], distance: Option[Double], pace: Option[Double], user: PublicUser, www: Option[String])

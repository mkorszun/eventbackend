package model.admin

import model.user.PublicUser

case class AdminEvent(headline: String, description: Option[String], timestamp: Long, x: Double, y: Double,
    tags: Option[Array[String]], distance: Int, pace: Option[Double], user: PublicUser)

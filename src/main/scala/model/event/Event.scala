package model.event

case class Event(headline: String, description: Option[String], timestamp: Long, x: Double, y: Double,
    tags: Option[Array[String]], distance: Double, pace: Option[Double])

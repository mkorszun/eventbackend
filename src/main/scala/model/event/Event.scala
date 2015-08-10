package model.event

case class Event(headline: String, description: String, timestamp: Long, x: Double, y: Double,
    tags: Array[String], distance: Int, pace: Double)

package model.event

case class Event(headline: String, description: String, date_and_time: String, x: Double, y: Double,
    tags: Array[String], distance: Int, pace: Double)

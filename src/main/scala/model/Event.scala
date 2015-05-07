package model

case class Event(headline: String, description: String, cost: Float, spots: Int,
    date_and_time: String, duration: Int, x: Double, y: Double, tags: Array[String])

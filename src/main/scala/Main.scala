import akka.actor._
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.mongodb.{BasicDBList, BasicDBObject, DBCollection}
import model.Event
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp {

    implicit val system = ActorSystem("my-system")

    startServer(interface = "0.0.0.0", port = System.getenv("PORT").toInt) {

        import format.EventJsonFormat._
        import spray.httpx.SprayJsonSupport._

        path("") {
            get {
                complete("OK")
            }
        } ~
            path("event") {
                post {
                    entity(as[Event]) {
                        event =>
                            val coll = getCollection();
                            val doc = new BasicDBObject()

                            doc.put("user_id", event.user_id)
                            doc.put("headline", event.headline)
                            doc.put("cost", event.cost)
                            doc.put("duration", event.duration)
                            doc.put("description", event.description)

                            val loc = new BasicDBObject()
                            loc.put("x", event.x)
                            loc.put("y", event.y)
                            doc.put("loc", loc)

                            coll.insert(doc)
                            complete("OK")
                    }
                } ~ get {
                    parameters('x.as[Long], 'y.as[Long]) {
                        (x, y) =>
                            val coll = getCollection()
                            val query = new BasicDBObject()
                            val loc = new BasicDBObject()
                            val near = new BasicDBList()
                            near.put("0", x)
                            near.put("1", y)
                            loc.put("$near", near)
                            query.put("loc", loc)

                            val results = coll.find(query).limit(50)
                            complete(com.mongodb.util.JSON.serialize(results))
                    }
                }
            }
    }

    private def getCollection(): DBCollection = {
        val uri = MongoClientURI(System.getenv("MONGOLAB_URI"))
        val mongoClient = MongoClient(uri)
        val db = mongoClient(uri.database.get)
        db.getCollection("events")
    }
}


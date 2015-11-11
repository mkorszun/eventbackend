package directives

import com.mongodb.DBCursor
import com.mongodb.casbah.Imports._
import model.APIResponse
import spray.routing.directives.RouteDirectives

class JsonDirective extends RouteDirectives {
    def apply(results: DBCursor): String = {
        com.mongodb.util.JSON.serialize(results)
    }

    def apply(result: DBObject): String = {
        com.mongodb.util.JSON.serialize(result)
    }

    def apply(result: Unit): APIResponse = {
        APIResponse("OK")
    }

    def apply(): APIResponse = {
        APIResponse("OK")
    }
}

class JsonEventDirective extends JsonDirective

class JsonUserDirective extends JsonDirective
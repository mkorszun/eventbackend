package service.http

import com.wordnik.swagger.annotations.{Api, ApiOperation}
import config.Config
import service.storage.tags.TagStorageService
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders.`Cache-Control`
import spray.routing._

@Api(value = "/event", description = "Tag actions", produces = "application/json", position = 1)
trait TagHTTPService extends HttpService with Config {

    implicit val tagService = TagStorageService

    def public_routes(): Route = {
        path("tags") {
            listAllTags
        }
    }

    @ApiOperation(
        httpMethod = "GET",
        value = "List all tags",
        response = classOf[String],
        responseContainer = "List")
    def listAllTags: Route = {
        import spray.json.DefaultJsonProtocol._
        import spray.json._
        get {
            respondWithHeader(`Cache-Control`(`max-age`(MAX_AGE_TAGS))) {
                complete {
                    tagService.allTags.toJson.toString()
                }
            }
        }
    }
}

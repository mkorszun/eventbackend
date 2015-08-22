package service.http

import com.wordnik.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import config.Config
import service.storage.tags.TagStorageService
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders.`Cache-Control`
import spray.routing._

@Api(value = "/tag", description = "Tag actions", produces = "application/json", position = 1)
trait TagHTTPService extends HttpService with Config {

    implicit val tagService = TagStorageService

    def public_routes(): Route = {
        path("tag") {
            pathEnd {
                listTags
            }
        }
    }

    @ApiOperation(
        httpMethod = "GET",
        value = "List tags - all or popular in given location",
        response = classOf[String],
        responseContainer = "List")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "x",
            value = "Latitude",
            required = false,
            dataType = "double",
            paramType = "query"),
        new ApiImplicitParam(
            name = "y",
            value = "Longitude",
            required = false,
            dataType = "double",
            paramType = "query"),
        new ApiImplicitParam(
            name = "max",
            value = "Max radius [m]",
            required = false,
            dataType = "integer",
            paramType = "query")))
    def listTags: Route = {
        import spray.json.DefaultJsonProtocol._
        import spray.json._
        get {
            parameters('x.as[Double], 'y.as[Double], 'max.as[Long]) {
                (x, y, max) =>
                    respondWithHeader(`Cache-Control`(`max-age`(MAX_AGE_TAGS))) {
                        complete {
                            tagService.findTags(x, y, max).toJson.toString()
                        }
                    }
            }
        }
    }
}

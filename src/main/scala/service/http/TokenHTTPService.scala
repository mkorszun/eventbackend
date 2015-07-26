package service.http

import auth.providers.FacebookProvider
import com.wordnik.swagger.annotations._
import spray.routing._

@Api(value = "/token", description = "Token actions", produces = "application/json", position = 1)
trait TokenHTTPService extends HttpService {

    def routes(): Route = {
        path("token") {
            createToken()
        }
    }

    @ApiOperation(
        httpMethod = "POST",
        value = "Create user access token")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "facebook_token",
            value = "Facebook token",
            required = true,
            dataType = "string",
            paramType = "query")
    ))
    def createToken(): Route = {
        post {
            parameters('facebook_token.as[String]) {
                (t) =>
                    complete(FacebookProvider.getOrCreate(t))
            }
        }
    }
}

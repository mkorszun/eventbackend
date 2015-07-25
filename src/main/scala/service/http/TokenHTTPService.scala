package service.http

import auth.providers.FacebookProvider
import com.wordnik.swagger.annotations._
import spray.routing._

@Api(value = "/token", description = "Operations about token.", produces = "application/json", position = 1)
trait TokenHTTPService extends HttpService {

    def routes(): Route = {
        path("token") {
            createUser()
        }
    }

    @ApiOperation(httpMethod = "POST", value = "Get or create user token")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(
            name = "facebook_token",
            value = "Facebook token",
            required = true,
            dataType = "string",
            paramType = "query")
    ))
    def createUser(): Route = {
        post {
            parameters('facebook_token.as[String]) {
                (t) =>
                    complete(FacebookProvider.getOrCreate(t))
            }
        }
    }
}

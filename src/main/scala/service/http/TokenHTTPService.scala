package service.http

import auth.providers.FacebookProvider
import com.wordnik.swagger.annotations._
import model.APIError
import spray.http.StatusCodes
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
            paramType = "query|body")
    ))
    def createToken(): Route = {
        import format.ErrorJsonFormat._
        import spray.httpx.SprayJsonSupport._
        post {
            parameters('facebook_token.as[String] ?) {
                (t) =>
                    formField('facebook_token.as[String] ?) {
                        (t1) => {
                            if (t.isEmpty && t1.isEmpty) {
                                complete((StatusCodes.BadRequest, APIError("Parameter missing: " + "facebook_token")))
                            } else {
                                val token = if (!t.isEmpty) t.get else t1.get
                                complete(FacebookProvider.getOrCreate(token))
                            }
                        }
                    }
            }
        }
    }
}

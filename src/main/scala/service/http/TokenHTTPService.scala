package service.http

import auth.BasicAuthenticator
import auth.providers.FacebookProvider
import com.wordnik.swagger.annotations._
import model.token.Token
import model.user.User
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global

@Api(value = "/token", description = "Token actions", produces = "application/json", position = 1)
trait TokenHTTPService extends HttpService with BasicAuthenticator {

    def routes(): Route = {
        path("token") {
            createToken()
        } ~ path("token1") {
            authenticate(basicUserAuthenticator) { user =>
                createToken1(user)
            }
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
        post {
            anyParams('facebook_token.as[String]) {
                (t) =>
                    complete(FacebookProvider.getOrCreate(t))
            }
        }
    }

    @ApiOperation(
        httpMethod = "POST",
        value = "Create user access token")
    def createToken1(user: User): Route = {
        import format.TokenJsonProtocol._
        import spray.httpx.SprayJsonSupport._
        post {
            complete {
                new Token(user.id, user.token)
            }
        }
    }
}

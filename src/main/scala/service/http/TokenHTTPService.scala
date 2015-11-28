package service.http

import javax.ws.rs.Path

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
        pathPrefix("token") {
            pathEnd {
                createToken()
            } ~ pathPrefix("v2") {
                authenticate(basicUserAuthenticator) { user =>
                    createToken1(user)
                }
            }
        }
    }

    @ApiOperation(
        httpMethod = "POST",
        value = "Create user access token with facebook token",
        response = classOf[Token])
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

    @Path("/v2")
    @ApiOperation(
        httpMethod = "POST",
        value = "Create user access token with email and password",
        response = classOf[Token])
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

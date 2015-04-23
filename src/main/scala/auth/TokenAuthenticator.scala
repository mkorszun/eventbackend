package auth

import spray.routing.authentication.{Authentication, ContextAuthenticator}
import spray.routing.{AuthenticationFailedRejection, RequestContext}

import scala.concurrent.{ExecutionContext, Future}

object TokenAuthenticator {

    object TokenExtraction {

        type TokenExtractor = RequestContext => Option[String]

        def fromHeader(headerName: String): TokenExtractor = { context: RequestContext =>
            context.request.headers.find(_.name == headerName).map(_.value)
        }

        def fromQueryString(parameterName: String): TokenExtractor = { context: RequestContext =>
            context.request.uri.query.get(parameterName)
        }
    }

    class TokenAuthenticator[T](extractor: TokenExtraction.TokenExtractor,
        authenticator: (String => Future[Option[T]]))
        (implicit executionContext: ExecutionContext) extends ContextAuthenticator[T] {

        import spray.routing.AuthenticationFailedRejection._

        def apply(context: RequestContext): Future[Authentication[T]] =
            extractor(context) match {
                case None =>
                    Future(
                        Left(AuthenticationFailedRejection(CredentialsMissing, List()))
                    )
                case Some(token) =>
                    authenticator(token) map {
                        case Some(t) =>
                            Right(t)
                        case None =>
                            Left(AuthenticationFailedRejection(CredentialsRejected, List()))
                    }
            }
    }

    def apply[T](headerName: String, queryStringParameterName: String)
        (authenticator: (String => Future[Option[T]]))
        (implicit executionContext: ExecutionContext) = {

        def extractor(context: RequestContext) =
            TokenExtraction.fromHeader(headerName)(context) orElse
                TokenExtraction.fromQueryString(queryStringParameterName)(context)

        new TokenAuthenticator(extractor, authenticator)
    }
}
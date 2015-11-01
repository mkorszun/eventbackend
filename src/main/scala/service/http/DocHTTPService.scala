package service.http

import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import config.Config
import spray.http.StatusCodes._
import spray.routing.{HttpService, Route}

import scala.reflect.runtime.universe._

trait DocHTTPService extends HttpService with Config {
    def routes(): Route = {
        new SwaggerHttpService {

            override def apiTypes = Seq(typeOf[EventHTTPService], typeOf[UserHTTPService], typeOf[TokenHTTPService],
                typeOf[TagHTTPService], typeOf[AdminHTTPService])

            override def apiVersion = "2.0"

            override def baseUrl = "/"

            override def docsPath = "doc"

            override def actorRefFactory = DocHTTPService.this.actorRefFactory

            override def apiInfo = Some(new ApiInfo(
                "Biegajmy event backend",
                "", "", "", "Apache V2",
                "http://www.apache.org/licenses/LICENSE-2.0"))
        }.routes ~ path("documentation") {
            redirect(DOCUMENTATION_URL, MovedPermanently)
        }
    }
}


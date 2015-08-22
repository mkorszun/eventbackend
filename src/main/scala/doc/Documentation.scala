package doc

import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import service.http.{EventHTTPService, TagHTTPService, TokenHTTPService, UserHTTPService}
import spray.routing.Route

import scala.reflect.runtime.universe._

object Documentation extends Documentation

class Documentation() {
    def docRoutes(): Route = {
        new SwaggerHttpService {

            override def apiTypes = Seq(typeOf[EventHTTPService], typeOf[UserHTTPService], typeOf[TokenHTTPService],
                typeOf[TagHTTPService])

            override def apiVersion = "2.0"

            override def baseUrl = "/"

            override def docsPath = "doc"

            override def actorRefFactory = actorRefFactory

            override def apiInfo = Some(new ApiInfo(
                "Biegajmy event backend",
                "", "", "", "Apache V2",
                "http://www.apache.org/licenses/LICENSE-2.0"))
        }.routes
    }
}


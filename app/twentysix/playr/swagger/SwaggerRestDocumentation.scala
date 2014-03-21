package twentysix.playr.swagger

import play.core.Router
import scala.runtime.AbstractPartialFunction
import scala.reflect.runtime.universe._
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger
import twentysix.playr._

case class SwaggerResource(path: String, description: String)
object SwaggerResource {
  implicit val jsonWrites = Json.writes[SwaggerResource]
}

case class SwaggerParameter(name: String, description: String, paramType: String="path", `type`: String="string", required: Boolean=true)
object SwaggerParameter {
  implicit val jsonWrites = Json.writes[SwaggerParameter]
}

case class SwaggerOperation(method: HttpMethod, nickname: String, summary: String, parameters: Seq[SwaggerParameter], dataType: String="string", `type`: String="string")
object SwaggerOperation {
  implicit val httpMethodJsonWrites = new Writes[HttpMethod] {
    def writes(method: HttpMethod) = JsString(method.name) 
  } 
  implicit val jsonWrites = Json.writes[SwaggerOperation]
  def simple(method: HttpMethod, nickname: String, parameters: Seq[SwaggerParameter]) = new SwaggerOperation(method, nickname, nickname, parameters)
}

case class SwaggerApi(path: String, description: String, operations: Traversable[SwaggerOperation])
object SwaggerApi {
  implicit val jsonWrites = Json.writes[SwaggerApi]
}

class SwaggerRestDocumentation(val restApi: RestRouter, val apiVersion: String="1.0") extends SimpleRouter {
  private val SubPathExpression = "^(/([^/]+)).*$".r

  val apiMap = restApi.routeResources("").map{ info =>
    info.path -> info
  }.toMap

  def apiList = apiMap.map { case(path, info) =>
    SwaggerResource(path, info.resourceType.toString())
  }

  def operationList(path: String, routeInfo: RestRouteInfo, parameters: Seq[SwaggerParameter] = Seq()): List[SwaggerApi] = {

    val bodyParam = SwaggerParameter("body", "body", "body")

    var res = List[SwaggerApi]()
    var ops = routeInfo.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.Read   => Some(SwaggerOperation.simple(GET, s"List ${routeInfo.name}", parameters))
        case ResourceCaps.Create => Some(SwaggerOperation.simple(POST, s"Create ${routeInfo.name}", parameters :+ bodyParam))
        case ResourceCaps.Action => 
          val method = routeInfo.asInstanceOf[ActionRestRouteInfo].method
          Some(SwaggerOperation.simple(method, routeInfo.name, if (Seq(POST, PUT, PATCH).contains(method)) parameters :+ bodyParam else parameters))
        case _ => None
      }
    }
    if(!ops.isEmpty)
      res = res :+ new SwaggerApi(path, "Generic operations", ops)

    val subParams = parameters :+ SwaggerParameter(s"${routeInfo.name}_id", s"identified ${routeInfo.name}")
    ops = routeInfo.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.Read   => Some(SwaggerOperation.simple(GET, s"Read ${routeInfo.name}", subParams))
        case ResourceCaps.Write  => Some(SwaggerOperation.simple(PUT, s"Write ${routeInfo.name}", subParams :+ bodyParam))
        case ResourceCaps.Update => Some(SwaggerOperation.simple(PATCH, s"Update ${routeInfo.name}", subParams :+ bodyParam))
        case ResourceCaps.Delete => Some(SwaggerOperation.simple(DELETE, s"Delete ${routeInfo.name}", subParams))
        case _ => None
      }
    }
    if(!ops.isEmpty)
      res = res :+ new SwaggerApi(s"$path/{${routeInfo.name}_id}", "Operations on identified resource", ops)

    res ++ routeInfo.subResources.flatMap(info => operationList(s"$path/{${routeInfo.name}_id}/${info.name}", info, subParams))
  }

  def resourceListing = Action {
    val res = Json.obj(
        "apiVersion" -> apiVersion,
        "swaggerVersion" -> "1.2",
        "apis" -> apiList
    )
    Results.Ok(Json.toJson(res))
  }

  def renderSwaggerUi = Action {
    Results.Ok(views.html.swagger(this.prefix+".json", this.prefix+"/ui"))
  }

  def resourceDesc(path: String, routeInfo: RestRouteInfo) = Action {
    val res = Json.obj(
        "apiVersion" -> apiVersion,
        "swaggerVersion" -> "1.2",
        "basePath" -> restApi.prefix,
        "resourcePath" -> path,
        "apis" -> operationList(path, routeInfo)
    )
    Results.Ok(Json.toJson(res))
  }

  private val ApiListing = "^\\.json(/.*)$".r
  private val UiAsset = "^/ui/(.*)$".r

  def routeRequest(header: RequestHeader, path: String, method: String): Option[Handler] = {
      path match {
        case ".json"         => Some(resourceListing)
        case ""|"/"          => Some(renderSwaggerUi)
        case ApiListing(api) => apiMap.get(api).map(resourceDesc(api, _))
        case UiAsset(asset)  => Some(controllers.Assets.at("/public/swagger-ui/dist", asset))
        case _               => None
      }
  }
}

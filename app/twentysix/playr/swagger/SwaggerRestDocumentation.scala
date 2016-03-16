package twentysix.playr.swagger

import scala.runtime.AbstractPartialFunction
import scala.reflect.runtime.universe._
import play.api.mvc._
import play.api.libs.json._
import twentysix.playr._
import scala.annotation.tailrec
import twentysix.playr.di.PlayRInfoConsumer

case class SwaggerResource(path: String, description: String)
object SwaggerResource {
  implicit val jsonWrites = Json.writes[SwaggerResource]
}

case class SwaggerParameter(name: String, description: String, paramType: String="path", `type`: String="string", required: Boolean=true)
object SwaggerParameter {
  implicit val jsonWrites = Json.writes[SwaggerParameter]
}

case class SwaggerOperation(method: HttpMethod,
                            nickname: String,
                            summary: String,
                            parameters: Seq[SwaggerParameter],
                            `type`: String="string",
                            consumes: Seq[String] = Seq("application/json", "application/xml", "text/plain"))
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

class SwaggerRestDocumentation(val apiPrefix: String, val restApi: RestRouter, val globalParameters: Seq[SwaggerParameter] = Seq(), val apiVersion: String="1.2") extends RouterWithPrefix {
  private val SubPathExpression = "^(/([^/]+)).*$".r

  def _apiSeq(root: String, info: RestRouteInfo): Seq[(String, RestRouteInfo)] = {
    val (nodes, leafs)  = info.subResources.partition(_.caps.contains(ResourceCaps.Api))
    leafs.map { sub => ( s"$root/${sub.name}" -> sub )} ++ nodes.flatMap { node=> _apiSeq(s"$root/${node.name}", node) }
  }

  val apiMap: Map[String, RestRouteInfo] = Map( _apiSeq("", restApi.routeResource) :_* )

  def apiList = apiMap.map { case(path, info) =>
    SwaggerResource(path, info.resourceType.toString())
  }

  def operationList(path: String, routeInfo: RestRouteInfo, parameters: Seq[SwaggerParameter] = Seq()): List[SwaggerApi] = {

    val bodyParam = SwaggerParameter("body", "body", "body")

    var res = List[SwaggerApi]()
    var ops = routeInfo.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.List   => Some(SwaggerOperation.simple(GET, s"List ${routeInfo.name}", parameters))
        case ResourceCaps.Create => Some(SwaggerOperation.simple(POST, s"Create ${routeInfo.name}", parameters :+ bodyParam))
        case ResourceCaps.Action =>
          routeInfo.asInstanceOf[ActionRestRouteInfo].methods.map { method =>
            SwaggerOperation.simple(method, routeInfo.name, if (Seq(POST, PUT, PATCH).contains(method)) parameters :+ bodyParam else parameters)
          }
        case _ => None
      }
    }
    if(ops.nonEmpty)
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
    if(ops.nonEmpty)
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

  def renderSwaggerUi(prefix: String) = Action {
    Results.Ok(views.html.swagger(prefix+".json", prefix+"/ui"))
  }

  def resourceDesc(path: String, routeInfo: RestRouteInfo) = Action {
    val res = Json.obj(
        "apiVersion" -> apiVersion,
        "swaggerVersion" -> "1.2",
        "basePath" -> apiPrefix,
        "resourcePath" -> path,
        "apis" -> operationList(path, routeInfo, globalParameters)
    )
    Results.Ok(Json.toJson(res))
  }

  private val ApiListing = "^\\.json(/.*)$".r
  private val UiAsset = "^/ui/(.*)$".r

  override def routesWithPrefix(prefix: String) = scala.Function.unlift{ requestHeader =>
    var swaggerUiVersion = "2.0.24"
    requestHeader.path match {
      case ".json"         => Some(resourceListing)
      case ""|"/"          => Some(renderSwaggerUi(apiPrefix+prefix))
      case ApiListing(api) => apiMap.get(api).map(resourceDesc(api, _))
      case UiAsset(asset)  => Some(controllers.Assets.at(path=s"/META-INF/resources/webjars/swagger-ui/${swaggerUiVersion}", file=asset))
      case _               => None
    }
  }

  def routes = routesWithPrefix("")
}

trait SwaggerDocumentation {
  this: RestRouter =>

  lazy val swaggerDoc = new SwaggerRestDocumentation("", this)
}

object SwaggerDocumentation extends PlayRInfoConsumer{
  def apply(prefix: String, router: RestRouter) = new SwaggerRestDocumentation(prefix, router)
}

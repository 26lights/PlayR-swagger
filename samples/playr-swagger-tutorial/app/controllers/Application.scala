package controllers

import play.api._
import play.api.mvc._
import twentysix.playr.RestApiRouter
import twentysix.playr.RestResourceRouter
import twentysix.playr.GET
import twentysix.playr.ApiInfo
import twentysix.playr.RootApiRouter
import twentysix.playr.SubRestResourceRouter
import models.Company
import javax.inject.Inject
import twentysix.playr.di._
import twentysix.playr.RestApiRouter
import play.api.cache.CacheApi
import models.ColorContainer
import models.EmployeeContainer
import models.CompanyContainer
import models.PersonContainer
import twentysix.playr.swagger.SwaggerDocumentation

class Application @Inject()(val cache: CacheApi) extends PlayRRouter with PlayRInfo {

  implicit val colorContainer = ColorContainer(cache)
  implicit val employeeContainer = EmployeeContainer(cache)
  implicit val personContainer = PersonContainer(cache)
  implicit val companyContainer = CompanyContainer(cache)

  val employeeApi = new SubRestResourceRouter[CompanyController, EmployeeController]("employee", (company: Company) => EmployeeController(company))
    .add("function", GET, (e: EmployeeController) => e.function _)

  val companyController = new CompanyController

  val crmApi = RestApiRouter("crm")
    .add(new PersonController)
    .add(new RestResourceRouter(companyController)
      .add("functions", GET, companyController.functions _)
      .add(employeeApi)
    )

  val api = RootApiRouter()
    .add(new ColorController)
    .add(crmApi)

  val info = Map(
    "info" -> ApiInfo,
    "docs" -> SwaggerDocumentation
  )
}

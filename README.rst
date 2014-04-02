================
Play'R - Swagger
================

Generate Swagger documentation for your Play'R defined ReST API.


This is an extension to the `Play'R project <https://github.com/26lights/PlayR>`_ 

.. warning::

  This project depends on a checkout of the swagger-ui project.

  To get this dependency, you have to run the following commands ::
  
    $ git clone https://github.com/26lights/PlayR-swagger.git
    $ cd PlayR-swagger
    $ git submodule update

How to use it
=============


First, you have to add ``playr-swagger`` to your build dependencies ( ``build.sbt`` ):

.. code-block:: scala

  resolvers += "26Lights snapshots" at "http://build.26source.org/nexus/content/repositories/public-snapshots"

  libraryDependencies += "26lights"  %% "playr-swagger"  % "0.1.0-SNAPSHOT"


Next, create an instance of ``SwaggerRestDocumentation`` for an existing Play'R router:

.. code-block:: scala

  ...
  import twentysix.playr.swagger.SwaggerRestDocumentation 
  ...
    val apidocs = new SwaggerRestDocumentation(router) // Generate swagger documentation
  ...

Example using ``playr-tutorial`` project's ``Application`` file:

.. code-block:: scala

  package controllers
  
  import play.api._
  import play.api.mvc._
  import twentysix.playr._
  import twentysix.playr.swagger.SwaggerRestDocumentation // Import 
  
  object Application extends Controller {
  
    val crmApi = RestApiRouter()
      .add(PersonController)
      .add(new RestResourceRouter(CompanyController)
        .add("employee", company => EmployeeController(company))
        .add("functions", "GET", CompanyController.functions)
      )
  
    val api = RestApiRouter()
      .add("crm" -> crmApi)
      .add(new RestResourceRouter(ColorController))
  
    val apidocs = new SwaggerRestDocumentation(api)
  }


And finally, just add a reference to the ``apidocs`` in your routes files.

Using the ``playr-tutorial`` project again: 

.. code-block:: nginx

  # Routes
  # This file defines all application routes (Higher priority routes first)
  # ~~~~

  ->      /api                          controllers.Application.api
  ->      /api-docs                     controllers.Application.apidocs

While running your application, if you point your browser to ``/api-docs``, you will get the swagger-ui interface with the generated documentation of your Play'R defined ReST api.

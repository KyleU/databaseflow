package controllers.admin

import better.files._
import controllers.BaseController
import models.schema.Schema
import services.connection.ConnectionSettingsService
import models.scalaexport.db.config.{ExportConfiguration, ExportConfigurationDefault}
import services.scalaexport.{ExportFiles, ExportHelper}
import services.scalaexport.db.ScalaExportService
import services.scalaexport.thrift.ThriftParseService
import services.schema.SchemaService
import util.ApplicationContext
import util.FutureUtils.defaultContext
import util.web.FormUtils
import util.JsonSerializers._

import scala.concurrent.Future

@javax.inject.Singleton
class ScalaExportController @javax.inject.Inject() (override val ctx: ApplicationContext) extends BaseController {
  def exportThrift(conn: String, filename: Option[String]) = withAdminSession("export.form") { implicit request =>
    ConnectionSettingsService.connFor(conn) match {
      case Some(cs) => filename match {
        case Some(fn) => SchemaService.getSchemaWithDetails(None, cs).map { schema =>
          val config = getConfig(schema)
          ExportFiles.prepareRoot()
          val flags = Set("rest", "graphql", "extras")
          val result = ThriftParseService.exportThrift(
            filename = fn, persist = true, projectLocation = config.projectLocation, flags = flags, configLocation = "./tmp/thrift"
          )
          Ok(views.html.admin.scalaExport.exportThrift(request.identity, fn.substring(fn.lastIndexOf('/') + 1), result._1, result._2))
        }
        case None => Future.successful(Ok(views.html.admin.scalaExport.thriftForm(request.identity, cs, File("./tmp/thrift"))))
      }
      case None => throw new IllegalStateException(s"Invalid connection [$conn].")
    }
  }

  def exportForm(conn: String) = withAdminSession("export.form") { implicit request =>
    ConnectionSettingsService.connFor(conn) match {
      case Some(cs) => SchemaService.getSchemaWithDetails(None, cs).map { schema =>
        Ok(views.html.admin.scalaExport.schemaForm(request.identity, cs, getConfig(schema), schema))
      }
      case None => throw new IllegalStateException(s"Invalid connection [$conn].")
    }
  }

  def export(conn: String) = withAdminSession("export.project") { implicit request =>
    val form = FormUtils.getForm(request)

    ConnectionSettingsService.connFor(conn) match {
      case Some(cs) => SchemaService.getSchemaWithDetails(None, cs).flatMap { schema =>
        val enums = schema.enums.map { e =>
          ScalaExportHelper.enumFor(e, form.filter(_._1.startsWith("enum." + e.key)).map(x => x._1.stripPrefix("enum." + e.key + ".") -> x._2))
        }
        val config = ExportConfiguration(
          key = ExportHelper.toIdentifier(schema.id),
          projectId = form("project.id"),
          projectTitle = form("project.title"),
          flags = form.getOrElse("project.flags", "").split(',').map(_.trim).filterNot(_.isEmpty).toSet,
          enums = enums,
          models = schema.tables.map { t =>
            val prefix = s"model.${t.name}."
            ScalaExportHelper.modelForTable(schema, t, form.filter(_._1.startsWith(prefix)).map(x => x._1.stripPrefix(prefix) -> x._2), enums)
          },
          source = form("project.source"),
          projectLocation = form.get("project.location").filter(_.nonEmpty),
          modelLocationOverride = form.get("model.location").filter(_.nonEmpty),
          thriftLocationOverride = form.get("thrift.location").filter(_.nonEmpty)
        )

        val x = config.asJson.spaces2
        configFileFor(schema).overwrite(x)

        ScalaExportService(config).export(persist = true).map { result =>
          Ok(views.html.admin.scalaExport.export(result.er, result.files, result.out))
        }
      }
      case None => throw new IllegalStateException(s"Invalid connection [$conn].")
    }
  }

  private[this] def getConfig(schema: Schema): ExportConfiguration = {
    val f = configFileFor(schema)
    if (f.exists) {
      ScalaExportHelper.merge(schema, decodeJson[ExportConfiguration](f.contentAsString) match {
        case Right(x) => x
        case Left(x) => throw x
      })
    } else {
      ExportConfigurationDefault.forSchema(schema)
    }
  }

  private[this] def configFileFor(schema: Schema) = {
    val schemaId = ExportHelper.toIdentifier(schema.id)
    val overrides = "./tmp/locations.txt".toFile.lines.map { l =>
      l.split('=').toList match {
        case h :: t :: Nil => h.trim -> t.trim
        case _ => throw new IllegalStateException(s"Unhandled line [$l].")
      }
    }.toMap
    (if (overrides.contains(schemaId)) { overrides(schemaId) + "/databaseflow.json" } else { s"./tmp/$schemaId.json" }).toFile
  }
}

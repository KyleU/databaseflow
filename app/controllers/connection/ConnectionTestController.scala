package controllers.connection

import java.util.UUID

import controllers.BaseController
import models.connection.ConnectionSettings
import models.engine.DatabaseEngine
import models.forms.ConnectionForm
import services.connection.ConnectionSettingsService
import services.database.DatabaseRegistry
import util.{ApplicationContext, PasswordEncryptUtils, SlugUtils}

import scala.concurrent.Future

@javax.inject.Singleton
class ConnectionTestController @javax.inject.Inject() (override val ctx: ApplicationContext) extends BaseController {
  def test(connectionId: UUID) = withSession("connection.test") { implicit request =>
    val result = ConnectionForm.form.bindFromRequest.fold(
      formWithErrors => {
        val errors = util.web.FormUtils.errorsToString(formWithErrors.errors)
        BadRequest(s"Invalid form: $errors")
      },
      cf => {
        val almostUpdated = ConnectionSettings(
          id = UUID.randomUUID,
          name = cf.name,
          slug = SlugUtils.slugFor(cf.name),
          owner = request.identity.id,
          engine = DatabaseEngine.withName(cf.engine),
          host = if (cf.isUrl) { None } else { cf.host },
          port = if (cf.isUrl) { None } else { cf.port },
          dbName = if (cf.isUrl) { None } else { cf.dbName },
          extra = if (cf.isUrl) { None } else { cf.extra },
          urlOverride = if (cf.isUrl) { cf.urlOverride } else { None },
          username = cf.username
        )
        val updated = if (cf.password.trim.isEmpty) {
          val connOpt = ConnectionSettingsService.getById(connectionId)
          almostUpdated.copy(password = connOpt match {
            case Some(c) => c.password
            case None => PasswordEncryptUtils.encrypt("")
          })
        } else {
          almostUpdated.copy(password = PasswordEncryptUtils.encrypt(cf.password))
        }
        val result = DatabaseRegistry.connect(updated, 1)
        result match {
          case Right(x) =>
            x._1.close()
            Ok("ok: " + x._2)
          case Left(x) => Ok("error: " + x.getMessage)
        }
      }
    )
    Future.successful(result)
  }
}

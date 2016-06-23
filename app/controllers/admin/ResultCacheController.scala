package controllers.admin

import java.util.UUID

import controllers.BaseController
import models.ddl.DdlQueries
import models.settings.SettingKey
import services.database.ResultCacheDatabase
import services.result.CachedResultService
import services.settings.SettingsService
import services.user.UserService
import utils.ApplicationContext

import scala.concurrent.Future

@javax.inject.Singleton
class ResultCacheController @javax.inject.Inject() (override val ctx: ApplicationContext, userService: UserService) extends BaseController {
  def results = withSession("admin-results") { implicit request =>
    val rows = CachedResultService.getAll
    val tables = CachedResultService.getTables
    Future.successful(Ok(views.html.admin.results(request.identity, ctx.config.debug, rows, tables)))
  }

  def removeResult(id: UUID) = withSession("admin-remove-result") { implicit request =>
    CachedResultService.remove(id)
    Future.successful(Redirect(controllers.admin.routes.ResultCacheController.results()).flashing("success" -> s"Removed result [$id]."))
  }

  def removeOrphan(id: String) = withSession("admin-remove-orphan") { implicit request =>
    ResultCacheDatabase.conn.executeUpdate(DdlQueries.DropTable(id)(ResultCacheDatabase.conn.engine))
    Future.successful(Redirect(controllers.admin.routes.ResultCacheController.results()).flashing("success" -> s"Removed orphan [$id]."))
  }
}
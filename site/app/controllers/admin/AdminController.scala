package controllers.admin

import java.util.UUID

import controllers.BaseSiteController
import licensing.LicenseEdition
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.Future

@javax.inject.Singleton
class AdminController @javax.inject.Inject() (implicit override val messagesApi: MessagesApi) extends BaseSiteController {
  def index() = withAdminSession("admin-index") { (username, request) =>
    implicit val req = request
    Future.successful(Ok(views.html.admin.index(username)))
  }

  def enable(id: String) = Action.async { implicit request =>
    if (request.getQueryString("p").contains("np")) {
      Future.successful(Redirect(controllers.admin.routes.AdminController.index()).withSession("admin-role" -> id))
    } else {
      Future.successful(Redirect(controllers.routes.SiteController.index()).withNewSession)
    }
  }

  def sandbox() = withAdminSession("admin-sandbox") { (username, request) =>
    implicit val req = request
    Future.successful(Ok(views.html.email.personalLicenseMessage(
      id = UUID.randomUUID,
      name = "License Holder",
      email = "email@databaseflow.com",
      edition = LicenseEdition.Personal.title,
      issued = 0,
      version = 1,
      content = "[License Content]"
    )))
  }
}

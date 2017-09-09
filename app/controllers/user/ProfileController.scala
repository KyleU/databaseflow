package controllers.user

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.BaseController
import models.user.UserForms
import play.api.i18n.Lang
import util.FutureUtils.defaultContext
import services.user.UserService
import util.ApplicationContext
import util.web.FormUtils

import scala.concurrent.Future

@javax.inject.Singleton
class ProfileController @javax.inject.Inject() (
    override val ctx: ApplicationContext,
    userService: UserService,
    authInfoRepository: AuthInfoRepository,
    credentialsProvider: CredentialsProvider,
    hasher: PasswordHasher
) extends BaseController {
  def view = withSession("view") { implicit request =>
    Future.successful(Ok(views.html.profile.view(request.identity)))
  }

  def save = withSession("view") { implicit request =>
    UserForms.profileForm.bindFromRequest.fold(
      _ => Future.successful(BadRequest(views.html.profile.view(request.identity))),
      profileData => {
        val newPrefs = request.identity.preferences.copy(
          theme = profileData.theme
        )
        val newUser = request.identity.copy(username = profileData.username, preferences = newPrefs)
        userService.save(newUser, update = true)
        Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
    )
  }

  def changePasswordForm = withSession("change-password-form") { implicit request =>
    Future.successful(Ok(views.html.profile.changePassword(request.identity)))
  }

  def changePassword = withSession("change-password") { implicit request =>
    def errorResponse(msg: String) = Redirect(controllers.user.routes.ProfileController.changePasswordForm()).flashing("error" -> msg)
    UserForms.changePasswordForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(errorResponse(FormUtils.errorsToString(formWithErrors.errors)))
      },
      changePass => {
        if (changePass.newPassword != changePass.confirm) {
          Future.successful(errorResponse("Passwords do not match."))
        } else {
          val email = request.identity.profile.providerKey
          credentialsProvider.authenticate(Credentials(email, changePass.oldPassword)).flatMap { loginInfo =>
            val okResponse = Redirect(controllers.user.routes.ProfileController.view()).flashing("success" -> messagesApi("user.password.changed")(
              request.lang
            ))
            for {
              _ <- authInfoRepository.update(loginInfo, hasher.hash(changePass.newPassword))
              authenticator <- ctx.silhouette.env.authenticatorService.create(loginInfo)
              result <- ctx.silhouette.env.authenticatorService.renew(authenticator, okResponse)
            } yield result
          }.recover {
            case e: ProviderException => errorResponse(messagesApi("user.password.check.fail", e.getMessage)(request.lang))
          }
        }
      }
    )
  }
}

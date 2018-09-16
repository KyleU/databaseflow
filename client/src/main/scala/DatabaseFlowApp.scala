import org.scalajs.jquery.{jQuery => $}
import scribe.Logging
import services.{InitService, NotificationService}

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel(name = "DatabaseFlow")
class DatabaseFlowApp extends Logging with NetworkHelper with ResponseMessageHelper {
  val debug = true

  InitService.init(sendMessage, connect _)

  logger.info("Database Flow has started.")

  protected[this] def handleServerError(reason: String, content: String) = {
    val lp = $("#loading-panel")
    val isLoading = lp.css("display") == "block"
    if (isLoading) {
      $("#tab-loading").text("Connection Error")
      val c = $("#loading-content", lp)
      c.text(s"Error loading database ($reason): $content")
    } else {
      NotificationService.error(reason, content)
    }
  }
}

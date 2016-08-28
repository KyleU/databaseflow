package services

import java.util.UUID

import models.RequestMessage
import models.query.RowDataOptions
import models.schema.FilterOp
import org.scalajs.dom
import org.scalajs.jquery.{jQuery => $}
import ui._
import ui.metadata.{MetadataManager, ModelListManager}
import ui.modal._
import ui.query._
import ui.search.SearchManager
import utils.{TemplateUtils, Logging, NetworkMessage}

import scala.scalajs.js

object InitService {
  def init(sendMessage: (RequestMessage) => Unit, connect: () => Unit) {
    Logging.installErrorHandler()
    NetworkMessage.register(sendMessage)
    wireSideNav()
    installTimers()

    TemplateUtils.clickHandler($("#commit-button"), (jq) => TransactionService.commitTransaction())
    TemplateUtils.clickHandler($("#rollback-button"), (jq) => TransactionService.rollbackTransaction())

    js.Dynamic.global.$("select").material_select()

    EditorCreationHelper.initEditorFramework()
    SearchManager.init()

    ShortcutService.init()
    ConfirmManager.init()
    ReconnectManager.init()
    QuerySaveFormManager.init()
    ShareResultsFormManager.init()
    QueryExportFormManager.init()
    PlanNodeDetailManager.init()
    Logging.debug("Database Flow has started.")
    connect()
  }

  private[this] def wireSideNav() = {
    TemplateUtils.clickHandler($("#begin-tx-link"), (jq) => TransactionService.beginTransaction())
    TemplateUtils.clickHandler($("#new-query-link"), (jq) => AdHocQueryManager.addNewQuery())
    TemplateUtils.clickHandler($(".show-list-link"), (jq) => ModelListManager.showList(jq.data("key").toString))
    TemplateUtils.clickHandler($("#sidenav-help-link"), (jq) => HelpManager.show())
    TemplateUtils.clickHandler($("#sidenav-feedback-link"), (jq) => FeedbackManager.show())
    TemplateUtils.clickHandler($("#sidenav-refresh-link"), (jq) => MetadataManager.refreshSchema())
    TemplateUtils.clickHandler($("#sidenav-history-link"), (jq) => HistoryManager.show())
    js.Dynamic.global.$(".button-collapse").sideNav()
  }

  def performInitialAction() = {
    TabManager.initIfNeeded()
    NavigationService.initialMessage match {
      case ("help", None) => HelpManager.show()
      case ("feedback", None) => FeedbackManager.show()
      case ("history", None) => HistoryManager.show()
      case ("list", Some(key)) => ModelListManager.showList(key)
      case ("new", None) => AdHocQueryManager.addNewQuery()
      case ("new", Some(id)) => AdHocQueryManager.addNewQuery(queryId = UUID.fromString(id))
      case ("saved-query", Some(id)) => SavedQueryManager.savedQueryDetail(UUID.fromString(id))
      case ("shared-result", Some(id)) => SharedResultManager.sharedResultDetail(UUID.fromString(id))
      case ("table", Some(id)) => id.indexOf("::") match {
        case -1 => TableManager.tableDetail(id, RowDataOptions.empty)
        case x =>
          val name = id.substring(0, x)
          val filter = id.substring(x + 2).split('=')
          val options = if (filter.length > 1) {
            RowDataOptions(
              filterCol = filter.headOption,
              filterOp = Some(FilterOp.Equal),
              filterVal = Some(filter.tail.mkString("="))
            )
          } else {
            Logging.info(s"Unable to parse filter [${filter.mkString("=")}].")
            RowDataOptions.empty
          }
          TableManager.tableDetail(name, options)
      }
      case ("view", Some(id)) => ViewManager.viewDetail(id)
      case ("procedure", Some(id)) => ProcedureManager.procedureDetail(id)
      case (key, id) =>
        Logging.info(s"Unhandled initial message [$key:${id.getOrElse("")}].")
        AdHocQueryManager.addNewQuery()
    }
  }

  def installTimers() = {
    dom.window.setInterval(TemplateUtils.relativeTime _, 1000)
  }
}

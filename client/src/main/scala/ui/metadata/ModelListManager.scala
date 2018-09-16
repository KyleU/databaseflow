package ui.metadata

import java.util.UUID

import models.query.RowDataOptions
import models.schema.Schema
import models.template.{Icons, ModelListTemplate}
import org.scalajs.jquery.{JQuery, jQuery => $}
import services.NavigationService
import ui.query._
import ui.WorkspaceManager
import ui.tabs.TabManager
import util.TemplateHelper

import scalatags.Text.all._

object ModelListManager {
  var openLists = Map.empty[String, UUID]

  private[this] def wire(queryPanel: JQuery, key: String) = TemplateHelper.clickHandler($(".list-link", queryPanel), jq => {
    val name = jq.data("name").toString
    key match {
      case "saved-query" => SavedQueryManager.savedQueryDetail(UUID.fromString(name))
      case "shared-result" => SharedResultManager.sharedResultDetail(UUID.fromString(name))
      case "table" => TableManager.tableDetail(name, RowDataOptions.empty)
      case "view" => ViewManager.viewDetail(name)
      case "procedure" => ProcedureManager.procedureDetail(name)
      case "enum" => EnumManager.enumDetail(name)
      case _ => throw new IllegalArgumentException(s"Invalid key [$key].")
    }
  })

  def showList(key: String) = openLists.get(key) match {
    case Some(queryId) => TabManager.selectTab(queryId)
    case None =>
      val queryId = UUID.randomUUID
      val schema = MetadataManager.schema.getOrElse(throw new IllegalStateException("Schema not available."))
      val (name, template) = getTemplate(key, queryId, schema)

      val panelHtml = div(id := s"panel-$queryId", cls := "workspace-panel")(template)
      WorkspaceManager.append(panelHtml.toString)

      def close() = if (QueryManager.closeQuery(queryId)) {
        openLists = openLists - key
      }

      TabManager.addTab(queryId, "list-" + key, name, Icons.list, close _)
      QueryManager.activeQueries = QueryManager.activeQueries :+ queryId

      val queryPanel = $(s"#panel-$queryId")
      if (queryPanel.length != 1) {
        throw new IllegalStateException(s"Found [${queryPanel.length}] query panels for model list.")
      }

      wire(queryPanel, key)
      val filterManager = ModelFilterManager(queryPanel)
      TemplateHelper.keyUpHandler($(".model-filter", queryPanel), (jq, key) => {
        if (key == 27) {
          jq.value("")
          filterManager.filter(None)
        } else {
          val v = jq.value().toString
          filterManager.filter(if (v.isEmpty) { None } else { Some(v.toLowerCase.trim) })
        }
      })

      openLists = openLists + (key -> queryId)
  }

  def updatePanel(key: String) = openLists.get(key) match {
    case Some(queryId) =>
      val schema = MetadataManager.schema.getOrElse(throw new IllegalStateException("Schema not available."))
      val (_, template) = getTemplate(key, queryId, schema)
      val queryPanel = $(s"#panel-$queryId")
      queryPanel.html(template.toString)
    case None => // no op
  }

  private[this] def getTemplate(key: String, queryId: UUID, schema: Schema) = key match {
    case "saved-query" => ModelListTemplate.forSavedQueries(
      queryId,
      SavedQueryManager.savedQueries.values.toSeq.sortBy(_.name),
      SavedQueryManager.usernameMap,
      NavigationService.connectionId
    )
    case "shared-result" => ModelListTemplate.forSharedResults(
      queryId,
      SharedResultManager.sharedResults.values.toSeq.sortBy(_.title),
      SharedResultManager.usernameMap
    )
    case "table" => ModelListTemplate.forTables(queryId, schema.tables.sortBy(_.name))
    case "view" => ModelListTemplate.forViews(queryId, schema.views.sortBy(_.name))
    case "procedure" => ModelListTemplate.forProcedures(queryId, schema.procedures.sortBy(_.name))
    case "enum" => ModelListTemplate.forEnums(queryId, schema.enums.sortBy(_.key))
    case _ => throw new IllegalArgumentException(s"Invalid key [$key].")
  }
}

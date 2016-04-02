package ui

import java.util.UUID

import models.query.SavedQuery
import models.template.{ Icons, QueryEditorTemplate }
import org.scalajs.jquery.{ JQueryEventObject, jQuery => $ }

object SavedQueryManager {
  var savedQueries = Map.empty[UUID, SavedQuery]
  var openSavedQueries = Set.empty[UUID]

  def savedQueryDetail(id: UUID) = openSavedQueries.find(_ == id) match {
    case Some(queryId) =>
      TabManager.selectTab(id)
    case None =>
      val savedQuery = savedQueries.getOrElse(id, throw new IllegalStateException(s"Unknown saved query [$id]."))
      addSavedQuery(savedQuery)
      openSavedQueries = openSavedQueries + id
  }

  private[this] def addSavedQuery(savedQuery: SavedQuery) = {
    QueryManager.workspace.append(QueryEditorTemplate.forSavedQuery(savedQuery.id, savedQuery.name, savedQuery.description, savedQuery.sql).toString)
    TabManager.addTab(savedQuery.id, savedQuery.name, Icons.savedQuery)

    val queryPanel = $(s"#panel-${savedQuery.id}")

    val sqlEditor =

      $(s".save-as-query-link", queryPanel).click({ (e: JQueryEventObject) =>
        QueryFormManager.show(savedQuery.copy(
          name = "Copy of " + savedQuery.name,
          sql = QueryManager.getSql(savedQuery.id)
        ))
        false
      })

    def onChange(s: String): Unit = {
      if (s == savedQuery.sql) {
        $(".unsaved-status", queryPanel).css("display", "none")
      } else {
        $(".unsaved-status", queryPanel).css("display", "inline")
      }
    }

    def onClose() = {
      openSavedQueries = openSavedQueries - savedQuery.id
    }

    QueryManager.addQuery(savedQuery.id, queryPanel, onChange, onClose)
  }
}
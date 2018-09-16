package ui.search

import models.template.Icons
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery => $}
import ui.metadata.{MetadataManager, ProcedureUpdates, TableUpdates, ViewUpdates}
import util.TemplateHelper

object SearchManager {
  private[this] lazy val searchContainer = $(".search-wrapper")
  private[this] lazy val searchInput = $("input#search", searchContainer)
  private[this] lazy val searchIcon = $(".fa", searchContainer)

  lazy val sharedResultsToggle = $("#shared-result-list-toggle")
  lazy val savedQueriesToggle = $("#saved-query-list-toggle")
  lazy val tablesToggle = $("#table-list-toggle")
  lazy val viewsToggle = $("#view-list-toggle")
  lazy val proceduresToggle = $("#procedure-list-toggle")

  private[this] var currentSearch = ""

  def init() = {
    if (searchContainer.length != 1 || searchInput.length != 1) {
      throw new IllegalStateException("Missing search input field.")
    }

    TemplateHelper.clickHandler(searchIcon, _ => {
      searchInput.value("")
      clearSearch()
      searchInput.trigger("focus")
      searchInput.trigger("blur")
    })

    searchInput.keyup { _: JQueryEventObject =>
      onTextChange(searchInput.value().toString.stripPrefix("/"))
    }
    searchInput.blur { _: JQueryEventObject =>
      onTextChange(searchInput.value().toString.stripPrefix("/"))
    }
  }

  def onTextChange(search: String) = if (currentSearch != search && MetadataManager.schema.isDefined) {
    val searches = search.toLowerCase.split(" ").map(_.trim).filter(_.nonEmpty)
    if (searches.isEmpty) {
      if (searchIcon.hasClass(Icons.close)) {
        searchIcon.removeClass(Icons.close).addClass(Icons.search).css("pointer", "default")
      }
      clearSearch()
    } else {
      if (searchIcon.hasClass(Icons.search)) {
        searchIcon.removeClass(Icons.search).addClass(Icons.close).css("pointer", "cursor")
      }
      SearchFilterManager.filterSchema(searches)
    }
    currentSearch = search
  }

  def focus() = {
    clearSearch()
    searchInput.focus()
  }

  def openIfClosed(j: JQuery) = if (!j.hasClass("active")) {
    $(".collapsible-header", j).trigger("click")
  }
  def closeIfOpen(j: JQuery) = if (j.hasClass("active")) {
    $(".collapsible-header", j).trigger("click")
  }

  def clearSearchEntries(o: Option[scala.Seq[(String, JQuery, JQuery)]], toggle: JQuery) = {
    closeIfOpen(toggle)
    o.foreach(_.foreach { x =>
      val name = x._2.data("name").toString
      x._3.text(name)
      x._2.attr("title", name)
      x._2.show()
    })
  }

  private[this] def clearSearch() = {
    $(".sidenav-link").removeClass("search-ignored")
    //clearSearchEntries(SharedResultManager.sharedResults, sharedResultsToggle)
    clearSearchEntries(MetadataManager.savedQueries, savedQueriesToggle)
    clearSearchEntries(TableUpdates.tables, tablesToggle)
    clearSearchEntries(ViewUpdates.views, viewsToggle)
    clearSearchEntries(ProcedureUpdates.procedures, proceduresToggle)
  }
}

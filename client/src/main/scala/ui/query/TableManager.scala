package ui.query

import java.util.UUID

import models.GetTableDetail
import models.engine.EngineQueries
import models.query.RowDataOptions
import models.schema.Table
import models.template._
import models.template.tbl.TableDetailTemplate
import org.scalajs.jquery.{ jQuery => $ }
import ui.metadata.MetadataManager
import ui.modal.QueryExportFormManager
import ui.{ UserManager, _ }

object TableManager extends TableDetailHelper {
  private[this] var openTables = Map.empty[String, UUID]

  def addTable(table: Table) = {
    openTables.get(table.name).foreach { uuid =>
      setTableDetails(uuid, table)
    }
  }

  def tableDetail(name: String, options: RowDataOptions) = {
    val qId = openTables.get(name) match {
      case Some(queryId) =>
        TabManager.selectTab(queryId)
        queryId
      case None =>
        val queryId = UUID.randomUUID
        WorkspaceManager.append(TableDetailTemplate.forTable(queryId, name).toString)

        MetadataManager.schema.flatMap(_.tables.find(_.name == name)) match {
          case Some(table) if table.columns.nonEmpty => setTableDetails(queryId, table)
          case _ => utils.NetworkMessage.sendMessage(GetTableDetail(name))
        }

        TabManager.addTab(queryId, "table-" + name, name, Icons.table)

        val queryPanel = $(s"#panel-$queryId")

        QueryManager.activeQueries = QueryManager.activeQueries :+ queryId

        utils.JQueryUtils.clickHandler($(".view-data-link", queryPanel), (jq) => {
          RowDataManager.showRowData("table", queryId, name, RowDataOptions(limit = Some(UserManager.rowsReturned)))
        })

        utils.JQueryUtils.clickHandler($(".export-link", queryPanel), (jq) => {
          implicit val engine = MetadataManager.engine.getOrElse(throw new IllegalStateException("Schema not initialized"))
          QueryExportFormManager.show(queryId, EngineQueries.selectFrom(name), name)
        })

        utils.JQueryUtils.clickHandler($(s".${Icons.close}", queryPanel), (jq) => {
          openTables = openTables - name
          QueryManager.closeQuery(queryId)
        })

        openTables = openTables + (name -> queryId)
        queryId
    }
    if (options.isFiltered) {
      RowDataManager.showRowData("table", qId, name, options.copy(limit = Some(UserManager.rowsReturned)))
    }
  }
}
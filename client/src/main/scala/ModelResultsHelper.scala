import java.util.UUID

import models._
import models.query.SavedQuery
import models.schema.{ Procedure, Table, View }
import org.scalajs.jquery.{ jQuery => $ }
import services.NotificationService
import ui._

trait ModelResultsHelper { this: DatabaseFlow =>
  private[this] var receivedSavedQueryResponse = false
  private[this] var receivedSchemaResultResponse = false

  protected[this] def handleBatchQueryStatus(bqs: BatchQueryStatus) = {
    SampleDatabaseManager.process(bqs)
  }

  protected[this] def handleSavedQueryResponse(sqrr: SavedQueryResultResponse) = {
    SavedQueryManager.updateSavedQueries(sqrr.savedQueries)
    if (!receivedSavedQueryResponse) {
      receivedSavedQueryResponse = true
      if (receivedSchemaResultResponse) {
        performInitialAction()
      }
    }
  }

  protected[this] def handleSchemaResultResponse(srr: SchemaResultResponse) = {
    if (MetadataManager.schema.isEmpty) {
      MetadataManager.updateSchema(srr.schema)
      $("#loading-panel").hide()
      if (!receivedSchemaResultResponse) {
        receivedSchemaResultResponse = true
        if (receivedSavedQueryResponse) {
          performInitialAction()
        }
      }
    } else {
      MetadataManager.updateSchema(srr.schema)
    }
  }

  protected[this] def handleTableResultResponse(t: Seq[Table]) = {
    MetadataUpdates.updateTables(t)
    t.foreach(TableManager.addTable)
  }
  protected[this] def handleViewResultResponse(v: Seq[View]) = {
    MetadataUpdates.updateViews(v)
    v.foreach(ViewManager.addView)
  }
  protected[this] def handleProcedureResultResponse(p: Seq[Procedure]) = {
    MetadataUpdates.updateProcedures(p)
    p.foreach(ProcedureManager.addProcedure)
  }

  protected[this] def handleQuerySaveResponse(sq: SavedQuery, error: Option[String]) = error match {
    case Some(err) => NotificationService.info("Query Save Error", err)
    case None => QuerySaveFormManager.handleQuerySaveResponse(sq, error)
  }

  protected[this] def handleQueryDeleteResponse(id: UUID, error: Option[String]) = error match {
    case Some(err) => NotificationService.info("Query Delete Error", err)
    case None => SavedQueryManager.deleteQuery(id)
  }
}

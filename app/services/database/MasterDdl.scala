package services.database

import models.database.Queryable
import models.ddl._
import util.Logging

object MasterDdl extends Logging {
  val tables = Seq(
    CreateUsersTable,
    CreatePasswordInfoTable,

    CreateConnectionsTable,
    CreateSavedQueriesTable,

    CreateQueryResultsTable,
    CreateSharedResultTable,

    CreateGraphQLTable,

    CreateSettingsTable,
    CreateAuditRecordTable
  )

  def update(q: Queryable) = {
    tables.foreach { t =>
      val exists = q.query(DdlQueries.DoesTableExist(t.tableName))
      if (!exists) {
        log.info(s"Creating missing table [${t.tableName}].")
        q.executeUpdate(t)
      }
    }
  }

  def wipe(q: Queryable) = {
    log.warn("Wiping database schema.")
    val tableNames = tables.reverseMap(_.tableName)
    tableNames.map { tableName =>
      q.executeUpdate(DdlQueries.TruncateTable(tableName))
    }
  }
}

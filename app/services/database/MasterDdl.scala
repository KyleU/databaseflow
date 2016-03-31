package services.database

import models.ddl._
import utils.Logging

object MasterDdl extends Logging {
  val tables = Seq(
    CreateUsersTable,
    CreateUserProfilesTable,
    CreateSessionInfoTable,
    CreatePasswordInfoTable,

    CreateConnectionsTable,
    CreateSavedQueriesTable
  )

  def update(db: DatabaseConnection) = {
    tables.foreach { t =>
      val exists = db.query(DdlQueries.DoesTableExist(t.tableName))
      if (exists) {
        Unit
      } else {
        log.info(s"Creating missing table [${t.tableName}].")
        db.execute(t)
      }
    }
  }

  def wipe(db: DatabaseConnection) = {
    log.warn("Wiping database schema.")
    val tableNames = tables.reverse.map(_.tableName)
    tableNames.map { tableName =>
      db.execute(DdlQueries.TruncateTable(tableName))
    }
  }
}

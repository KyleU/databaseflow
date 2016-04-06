package services.schema

import java.sql.Connection
import java.util.UUID

import models.schema.{ Procedure, Schema, Table }
import services.database.DatabaseConnection

object SchemaService {
  private[this] var schemas: Map[UUID, Schema] = Map.empty
  private[this] var tables = Map.empty[String, Table]
  private[this] var views = Map.empty[String, Table]
  private[this] var procedures = Map.empty[String, Procedure]

  def getTable(connectionId: UUID, name: String) = tables.get(name)
  def getView(connectionId: UUID, name: String) = views.get(name)
  def getProcedure(connectionId: UUID, name: String) = procedures.get(name)

  private[this] def withConnection[T](db: DatabaseConnection)(f: (Connection) => T) = {
    val conn = db.source.getConnection()
    try {
      f(conn)
    } finally {
      conn.close()
    }
  }

  def getSchema(connectionId: UUID, db: DatabaseConnection, forceRefresh: Boolean = false) = schemas.get(connectionId) match {
    case Some(schema) if !forceRefresh => schema
    case None => calculateSchema(connectionId, db)
  }

  private[this] def calculateSchema(connectionId: UUID, db: DatabaseConnection) = withConnection(db) { conn =>
    val catalogName = Option(conn.getCatalog)
    val schemaName = try {
      Option(conn.getSchema)
    } catch {
      case _: AbstractMethodError => None
    }
    val metadata = conn.getMetaData

    val schemaModel = Schema(
      connectionId = connectionId,
      schemaName = schemaName,
      catalog = catalogName,
      url = metadata.getURL,
      username = metadata.getUserName,
      engine = db.engine.id,
      engineVersion = metadata.getDatabaseProductVersion,
      driver = metadata.getDriverName,
      driverVersion = metadata.getDriverVersion,
      catalogTerm = metadata.getCatalogTerm,
      schemaTerm = metadata.getSchemaTerm,
      procedureTerm = metadata.getProcedureTerm,
      maxSqlLength = metadata.getMaxStatementLength,

      tables = Nil,
      views = Nil,
      procedures = Nil
    )

    val tables = MetadataTables.getTableNames(metadata, catalogName, schemaName, "TABLE")
    val views = MetadataTables.getTableNames(metadata, catalogName, schemaName, "VIEW")
    val procedures = MetadataProcedures.getProcedureNames(metadata, catalogName, schemaName)

    val ret = schemaModel.copy(
      tables = tables,
      views = views,
      procedures = procedures
    )
    schemas = schemas + (connectionId -> ret)
    ret
  }

  def getTableDetail(connectionId: UUID, db: DatabaseConnection, name: String, forceRefresh: Boolean = false) = {
    val sch = getSchema(connectionId, db)
    tables.get(name) match {
      case Some(p) if !forceRefresh => p
      case _ => withConnection(db) { conn =>
        val ret = MetadataTables.getTableDetails(db, conn, conn.getMetaData, sch.catalog, sch.schemaName, name)
        tables = tables + (name -> ret)
        ret
      }
    }
  }

  def getViewDetail(connectionId: UUID, db: DatabaseConnection, name: String, forceRefresh: Boolean = false) = {
    val sch = getSchema(connectionId, db)
    views.get(name) match {
      case Some(p) if !forceRefresh => p
      case _ => withConnection(db) { conn =>
        val ret = MetadataTables.getViewDetails(db, conn, conn.getMetaData, sch.catalog, sch.schemaName, name)
        views = views + (name -> ret)
        ret
      }
    }
  }

  def getProcedureDetail(connectionId: UUID, db: DatabaseConnection, name: String, forceRefresh: Boolean = false) = {
    val sch = getSchema(connectionId, db)
    procedures.get(name) match {
      case Some(p) if !forceRefresh => p
      case _ => withConnection(db) { conn =>
        val ret = MetadataProcedures.getProcedureDetails(conn.getMetaData, sch.catalog, sch.schemaName, name)
        procedures = procedures + (name -> ret)
        ret
      }
    }
  }
}

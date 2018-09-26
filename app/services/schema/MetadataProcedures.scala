package services.schema

import java.sql.DatabaseMetaData

import models.database.Row
import models.queries.QueryTranslations
import models.schema.{EnumType, Procedure, ProcedureParam}
import util.{Logging, NullUtils}

import scala.util.control.NonFatal

object MetadataProcedures extends Logging {
  def getProcedures(metadata: DatabaseMetaData, catalog: Option[String], schema: Option[String]) = {
    val rs = metadata.getProcedures(catalog.orNull, schema.orNull, NullUtils.inst)
    new Row.Iter(rs).map(procedureFromRow).toList.sortBy(_.name)
  }

  def withProcedureDetails(metadata: DatabaseMetaData, catalog: Option[String], schema: Option[String], procedures: Seq[Procedure], enums: Seq[EnumType]) = {
    procedures.zipWithIndex.map { p =>
      if (p._2 > 0 && p._2 % 25 == 0) { log.info(s"Processed [${p._2}/${procedures.size}] procedures...") }
      getProcedureDetails(metadata, catalog, schema, p._1, enums)
    }
  }

  def getProcedureDetails(metadata: DatabaseMetaData, catalog: Option[String], schema: Option[String], procedure: Procedure, enums: Seq[EnumType]) = try {
    val rs2 = metadata.getProcedureColumns(catalog.orNull, schema.orNull, procedure.name, NullUtils.inst)
    val columns = new Row.Iter(rs2).map(row => columnFromRow(row, enums)).toList.sortBy(_._1).map(_._2)
    procedure.copy(params = columns)
  } catch {
    case NonFatal(x) =>
      log.info(s"Unable to get procedure details for [${procedure.name}].", x)
      procedure
  }

  private[this] def procedureFromRow(row: Row) = {
    val procType = JdbcHelper.intVal(row.as[Any]("procedure_type"))
    Procedure(
      name = row.as[String]("procedure_name"),
      description = row.asOpt[String]("remarks"),
      params = Nil,
      returnsResult = procType match {
        case DatabaseMetaData.procedureResultUnknown => None
        case DatabaseMetaData.procedureReturnsResult => Some(true)
        case DatabaseMetaData.procedureNoResult => Some(false)
        case x => throw new IllegalArgumentException(x.toString)
      }
    )
  }

  private[this] def columnFromRow(row: Row, enums: Seq[EnumType]) = {
    val paramType = JdbcHelper.intVal(row.as[Any]("COLUMN_TYPE"))
    val colType = JdbcHelper.intVal(row.as[Any]("DATA_TYPE"))
    val colTypeName = row.asOpt[Any]("TYPE_NAME").map(x => JdbcHelper.stringVal(x)).getOrElse("")

    row.as[Int]("ORDINAL_POSITION") -> ProcedureParam(
      name = row.as[String]("COLUMN_NAME"),
      description = row.asOpt[String]("REMARKS"),
      paramType = paramType match {
        case DatabaseMetaData.procedureColumnUnknown => "unknown"
        case DatabaseMetaData.procedureColumnIn => "in"
        case DatabaseMetaData.procedureColumnInOut => "inout"
        case DatabaseMetaData.procedureColumnOut => "out"
        case DatabaseMetaData.procedureColumnReturn => "return"
        case DatabaseMetaData.procedureColumnResult => "result"
        case _ => "?"
      },
      columnType = QueryTranslations.forType(colType, colTypeName, None, enums),
      sqlTypeCode = colType, // SQL_DATA_TYPE? SOURCE_DATA_TYPE?
      sqlTypeName = colTypeName,
      nullable = row.as[String]("IS_NULLABLE") == "YES"
    )
  }
}

package services.schema

import java.sql.DatabaseMetaData

import models.database.Row
import models.schema.{ Column, Table }
import utils.NullUtils

object MetadataColumns {
  def getColumns(metadata: DatabaseMetaData, t: Table) = {
    val rs = metadata.getColumns(t.catalog.orNull, t.schema.orNull, t.name, NullUtils.inst)
    val columns = new Row.Iter(rs).map { row =>
      row.as[Int]("ORDINAL_POSITION") -> Column(
        name = row.as[String]("COLUMN_NAME"),
        description = row.asOpt[String]("REMARKS"),
        definition = row.asOpt[String]("COLUMN_DEF"),
        primaryKey = false, //row.as[Boolean]("?"),
        notNull = row.as[Int]("NULLABLE") == 0, // IS_NULLABLE?
        autoIncrement = row.as[String]("IS_AUTOINCREMENT") == "YES",
        typeCode = row.as[Int]("DATA_TYPE"), // SQL_DATA_TYPE? SOURCE_DATA_TYPE?
        typeName = row.as[String]("TYPE_NAME"),
        size = row.asOpt[Int]("COLUMN_SIZE").map(_.toString).getOrElse("?"),
        sizeAsInt = row.asOpt[Int]("COLUMN_SIZE").getOrElse(0), // ?
        scale = 0, // BUFFER_LENGTH? DECIMAL_DIGITS? NUM_PREC_RADIX?
        defaultValue = None // row.asOpt[String]("?")
      )
    }.toList
    columns.sortBy(_._1).map(_._2)
  }
}
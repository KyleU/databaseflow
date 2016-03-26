package models.queries

import java.sql.Types._

import models.schema.ColumnType._

object QueryTranslations {
  def forType(i: Int) = i match {
    case CHAR | VARCHAR | LONGVARCHAR | CLOB | NCHAR | NVARCHAR | LONGNVARCHAR | NCLOB => StringType
    case NUMERIC | DECIMAL => BigDecimalType
    case BIT | BOOLEAN => BooleanType
    case TINYINT => ByteType
    case SMALLINT => ShortType
    case INTEGER | DISTINCT | ROWID => IntegerType
    case BIGINT => LongType
    case REAL | FLOAT => FloatType
    case DOUBLE => DoubleType
    case BINARY | VARBINARY | LONGVARBINARY | BLOB => ByteArrayType
    case DATE => DateType
    case TIME | TIME_WITH_TIMEZONE => TimeType
    case TIMESTAMP | TIMESTAMP_WITH_TIMEZONE => TimestampType
    case NULL => NullType
    case JAVA_OBJECT => ObjectType
    case STRUCT => StructType
    case ARRAY => ArrayType
    case REF | REF_CURSOR => RefType
    case SQLXML => XmlType
    case _ => UnknownType
  }
}
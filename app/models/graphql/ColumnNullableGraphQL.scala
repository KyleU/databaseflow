package models.graphql

import java.util.UUID

import models.result.QueryResultRow
import models.schema.ColumnType
import sangria.schema._

object ColumnNullableGraphQL {
  private[this] def getDefaultField(name: String, description: Option[String], cleanName: String) = Field(
    name = cleanName, fieldType = OptionType(StringType), description = description,
    resolve = (x: Context[GraphQLContext, QueryResultRow]) => x.value.getCell(name)
  )

  private[this] def getTypedField[T](name: String, description: Option[String], cleanName: String, t: ScalarType[T], f: String => T) = Field(
    name = cleanName, fieldType = OptionType(t), description = description,
    resolve = (x: Context[GraphQLContext, QueryResultRow]) => x.value.getCell(name).map(f)
  )

  def getColumnField(name: String, description: Option[String], columnType: ColumnType, cleanName: String, sqlTypeName: String) = columnType match {
    case ColumnType.StringType => getDefaultField(name, description, cleanName)
    case ColumnType.EncryptedStringType => getDefaultField(name, description, cleanName)

    case ColumnType.BooleanType => getTypedField[Boolean](name, description, cleanName, BooleanType, _ == "true")
    case ColumnType.ByteType => getTypedField[Byte](name, description, cleanName, CommonGraphQL.byteType, _.toByte)
    case ColumnType.ShortType => getTypedField[Short](name, description, cleanName, CommonGraphQL.shortType, _.toShort)
    case ColumnType.IntegerType => getTypedField[Int](name, description, cleanName, IntType, _.toInt)
    case ColumnType.LongType => getTypedField[Long](name, description, cleanName, LongType, _.toLong)
    case ColumnType.FloatType => getTypedField[Double](name, description, cleanName, FloatType, _.toDouble)
    case ColumnType.BigDecimalType | ColumnType.DoubleType => getTypedField[BigDecimal](name, description, cleanName, BigDecimalType, BigDecimal.apply)

    case ColumnType.DateType | ColumnType.TimeType | ColumnType.TimestampType => getDefaultField(name, description, cleanName)
    case ColumnType.TimestampZonedType => getDefaultField(name, description, cleanName)

    case ColumnType.RefType | ColumnType.XmlType => getDefaultField(name, description, cleanName)
    case ColumnType.UuidType => getTypedField[UUID](name, description, cleanName, CommonGraphQL.uuidType, UUID.fromString)

    case ColumnType.ObjectType | ColumnType.StructType => getDefaultField(name, description, cleanName)
    case ColumnType.JsonType => getDefaultField(name, description, cleanName)

    case ColumnType.EnumType => getDefaultField(name, description, cleanName)
    case ColumnType.CodeType => getDefaultField(name, description, cleanName)
    case ColumnType.TagsType => getDefaultField(name, description, cleanName)

    case ColumnType.ArrayType => ArrayGraphQL.getArrayField(name, description, cleanName, sqlTypeName)
    case ColumnType.ByteArrayType => getDefaultField(name, description, cleanName)

    case ColumnType.UnknownType => getDefaultField(name, description, cleanName)
  }
}

package com.databaseflow.models.scalaexport.db

import com.databaseflow.models.scalaexport.file.ScalaFile
import models.schema.ColumnType
import models.schema.ColumnType._
import com.databaseflow.services.scalaexport.ExportHelper
import util.JsonSerializers._

object ExportField {
  implicit val jsonEncoder: Encoder[ExportField] = deriveEncoder
  implicit val jsonDecoder: Decoder[ExportField] = deriveDecoder

  def getDefaultString(providedPrefix: String, t: ColumnType, enumOpt: Option[ExportEnum], defaultValue: Option[String]) = t match {
    case BooleanType => defaultValue.map(v => if (v == "1" || v == "true") { "true" } else { "false" }).getOrElse("false")
    case ByteType => defaultValue.filter(_.matches("[0-9]+")).getOrElse("0")
    case IntegerType => defaultValue.filter(_.matches("[0-9]+")).getOrElse("0")
    case LongType => defaultValue.filter(_.matches("[0-9]+")).getOrElse("0") + "L"
    case ShortType => defaultValue.filter(_.matches("[0-9]+")).getOrElse("0") + ".toShort"
    case FloatType => defaultValue.filter(_.matches("[0-9\\.]+")).getOrElse("0.0") + "f"
    case DoubleType => defaultValue.filter(_.matches("[0-9\\.]+")).getOrElse("0.0")
    case BigDecimalType => s"BigDecimal(${defaultValue.filter(_.matches("[0-9\\.]+")).getOrElse("0")})"

    case DateType => providedPrefix + "util.DateUtils.today"
    case TimeType => providedPrefix + "util.DateUtils.currentTime"
    case TimestampType => providedPrefix + "util.DateUtils.now"
    case TimestampZonedType => providedPrefix + "util.DateUtils.nowZoned"

    case UuidType => defaultValue.filter(_.length == 36).map(d => s"""UUID.fromString("$d")""").getOrElse("UUID.randomUUID")

    case JsonType => "Json.obj()"
    case ArrayType => "Seq.empty"
    case TagsType => s"Seq.empty[${providedPrefix}models.tag.Tag]"
    case EnumType => enumOpt match {
      case Some(enum) =>
        val (_, cn) = defaultValue.flatMap(d => enum.valuesWithClassNames.find(_._1 == d)).getOrElse {
          enum.valuesWithClassNames.headOption.getOrElse(throw new IllegalStateException(s"No enum values for [${enum.name}]."))
        }
        s"${enum.className}.$cn"
      case None => "\"" + defaultValue.getOrElse("") + "\""
    }
    case _ => "\"" + defaultValue.getOrElse("") + "\""
  }
}

case class ExportField(
    columnName: String,
    propertyName: String,
    title: String,
    fkNameOverride: String = "",
    description: Option[String],
    idx: Int = 0,
    t: ColumnType,
    sqlTypeName: String,
    enumOpt: Option[ExportEnum] = None,
    defaultValue: Option[String] = None,
    notNull: Boolean = false,
    indexed: Boolean = false,
    unique: Boolean = false,
    inSearch: Boolean = false,
    inView: Boolean = true,
    inSummary: Boolean = false,
    ignored: Boolean = false
) {
  val nullable = !notNull

  val className = enumOpt.map(_.className).getOrElse(ExportHelper.toClassName(propertyName))
  def classNameForSqlType = t match {
    case EnumType => enumOpt.map { e =>
      s"EnumType(${e.className})"
    }.getOrElse(throw new IllegalStateException(s"Cannot find enum matching [$sqlTypeName]."))
    case ArrayType => ArrayType.typForSqlType(sqlTypeName)
    case _ => t.className
  }

  val scalaType = enumOpt.map(_.className).getOrElse(t.asScala)
  val scalaTypeFull = enumOpt.map(e => e.modelPackage match {
    case Nil => e.className
    case pkg => pkg.mkString(".") + "." + e.className
  }).getOrElse(t.asScalaFull)

  val graphQlArgType = ExportFieldGraphQL.argType(this)
  val graphQlSeqArgType = ExportFieldGraphQL.listArgType(this)

  val thriftType = ExportFieldThrift.thriftType(t, sqlTypeName, enumOpt)
  val thriftVisibility = if (notNull) { "required" } else { "optional" }

  def addImport(file: ScalaFile, pkg: Seq[String] = Nil) = {
    enumOpt match {
      case Some(enum) if enum.modelPackage == pkg => // noop
      case Some(enum) => file.addImport(enum.modelPackage.mkString("."), scalaType)
      case None => t.requiredImport.foreach(pkg => file.addImport(pkg, scalaType))
    }
  }

  def defaultString(providedPrefix: String) = ExportField.getDefaultString(providedPrefix, t, enumOpt, defaultValue)

  def fromString(s: String) = enumOpt.map { enum =>
    s"${enum.className}.withValue($s)"
  }.getOrElse(t.fromString.replaceAllLiterally("xxx", s))
}

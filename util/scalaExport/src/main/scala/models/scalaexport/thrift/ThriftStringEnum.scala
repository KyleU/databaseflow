package models.scalaexport.thrift

import com.facebook.swift.parser.model.StringEnum
import services.scalaexport.db.ExportHelper

import scala.collection.JavaConverters._

case class ThriftStringEnum(e: StringEnum) {
  val name = ExportHelper.toClassName(e.getName)
  val values = e.getValues.asScala
}

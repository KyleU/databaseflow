package models.scalaexport.thrift

import com.facebook.swift.parser.model.Struct
import services.scalaexport.db.ExportHelper

import scala.collection.JavaConverters._

case class ThriftStruct(s: Struct) {
  val name = ExportHelper.toClassName(s.getName)
  val fields = s.getFields.asScala.map(ThriftStructField.apply)
  val identifier = ExportHelper.toIdentifier(name)
}

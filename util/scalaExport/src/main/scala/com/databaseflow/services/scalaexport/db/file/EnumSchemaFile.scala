package com.databaseflow.services.scalaexport.db.file

import com.databaseflow.models.scalaexport.db.ExportEnum
import com.databaseflow.models.scalaexport.db.config.ExportConfiguration
import com.databaseflow.models.scalaexport.file.ScalaFile

object EnumSchemaFile {
  def export(config: ExportConfiguration, enum: ExportEnum) = {
    val file = ScalaFile(enum.modelPackage, enum.className + "Schema", None)
    file.addImport(config.providedPrefix + "graphql", "CommonSchema")
    file.addImport(config.corePrefix + "graphql", "GraphQLContext")
    file.addImport(config.corePrefix + "graphql", "GraphQLSchemaHelper")
    file.addImport("sangria.schema", "EnumType")
    file.addImport("sangria.schema", "ListType")
    file.addImport("sangria.schema", "fields")
    file.addImport("scala.concurrent", "Future")

    file.add(s"""object ${enum.className}Schema extends GraphQLSchemaHelper("${enum.propertyName}") {""", 1)
    file.add(s"implicit val ${enum.propertyName}EnumType: EnumType[${enum.className}] = CommonSchema.deriveStringEnumeratumType(", 1)
    file.add(s"""name = "${enum.className}",""")
    file.add(s"values = ${enum.className}.values")
    file.add(")", -1)
    file.add()

    file.add("val queryFields = fields(", 1)
    val r = s"""Future.successful(${enum.className}.values)"""
    file.add(s"""unitField(name = "${enum.propertyName}", desc = None, t = ListType(${enum.propertyName}EnumType), f = (_, _) => $r)""")
    file.add(")", -1)
    file.add("}", -1)

    file
  }
}

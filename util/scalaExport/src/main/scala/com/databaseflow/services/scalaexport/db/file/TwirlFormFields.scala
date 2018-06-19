package com.databaseflow.services.scalaexport.db.file

import com.databaseflow.models.scalaexport.db.{ExportField, ExportModel}
import com.databaseflow.models.scalaexport.file.OutputFile
import models.schema.{ColumnType, ForeignKey}

object TwirlFormFields {
  def fieldFor(providedPrefix: String, model: ExportModel, field: ExportField, file: OutputFile, autocomplete: Option[(ForeignKey, ExportModel)]) = {
    field.t match {
      case ColumnType.EnumType => file.add(s"@views.html.components.form.selectField(${enumArgsFor(field)})")
      case ColumnType.CodeType => file.add(s"@views.html.components.form.codeField(${argsFor(field)})")
      case ColumnType.BooleanType => file.add(s"@views.html.components.form.booleanField(${boolArgsFor(field)})")
      case ColumnType.DateType => timeField(field, file, "Date")
      case ColumnType.TimeType => timeField(field, file, "Time")
      case ColumnType.TimestampType => timeField(field, file, "DateTime")
      case _ if autocomplete.isDefined => autocompleteField(providedPrefix, field, autocomplete.get, file)
      case _ => file.add(s"@views.html.components.form.textField(${argsFor(field)})")
    }
  }

  private[this] def argsFor(field: ExportField) = {
    val prop = field.propertyName
    val valString = if (field.notNull) { s"Some(model.$prop.toString)" } else { s"""model.$prop.map(_.toString)""" }
    val dataTypeString = if (field.t == ColumnType.StringType) { "" } else { s""", dataType = "${field.t}"""" }
    s"""selected = isNew, key = "$prop", title = "${field.title}", value = $valString, nullable = ${field.nullable}$dataTypeString"""
  }

  private[this] def boolArgsFor(field: ExportField) = {
    val prop = field.propertyName
    val valString = if (field.notNull) { s"Some(model.$prop)" } else { s"""model.$prop""" }
    val dataTypeString = if (field.t == ColumnType.StringType) { "" } else { s""", dataType = "${field.t}"""" }
    s"""selected = isNew, key = "$prop", title = "${field.title}", value = $valString, nullable = ${field.nullable}$dataTypeString"""
  }

  private[this] def enumArgsFor(field: ExportField) = {
    val enum = field.enumOpt.getOrElse(throw new IllegalStateException(s"Cannot find enum with name [${field.sqlTypeName}]."))
    val prop = field.propertyName
    val valString = if (field.notNull) { s"Some(model.$prop.toString)" } else { s"""model.$prop.map(_.toString)""" }
    val opts = "Seq(" + enum.values.map(v => s"""("$v" -> "$v")""").mkString(", ") + ")"
    s"""selected = isNew, key = "$prop", title = "${field.title}", value = $valString, options = $opts, nullable = ${field.nullable}, dataType = "${enum.name}""""
  }

  private[this] def timeField(field: ExportField, file: OutputFile, t: String) = {
    val prop = field.propertyName
    val valString = if (field.notNull) { s"Some(model.$prop)" } else { s"""model.$prop""" }
    val args = s"""selected = isNew, key = "$prop", title = "${field.title}", value = $valString, nullable = ${field.nullable}"""
    file.add(s"@views.html.components.form.local${t}Field($args)")
  }

  private[this] def autocompleteField(providedPrefix: String, field: ExportField, autocomplete: (ForeignKey, ExportModel), file: OutputFile) = {
    file.add(s"@views.html.components.form.autocompleteField(", 1)
    file.add(argsFor(field) + ",")
    val url = s"${autocomplete._2.routesClass}.autocomplete()"
    val icon = providedPrefix + s"models.template.Icons.${autocomplete._2.propertyName}"
    file.add(s"""call = $url, acType = ("${autocomplete._2.propertyName}", "${autocomplete._2.title}"), icon = $icon""")
    file.add(")", -1)
  }
}

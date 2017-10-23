package services.scalaexport.file

import models.scalaexport.TwirlFile
import services.scalaexport.config.{ExportConfiguration, ExportModel}

object TwirlDataRowFile {
  def export(config: ExportConfiguration, model: ExportModel) = {
    val searchColumns = model.fields.filter(_.inSearch)

    val file = TwirlFile(model.viewPackage, model.propertyName + "DataRow")
    file.add(s"@(model: ${model.modelClass})<tr>", 1)
    searchColumns.foreach { c =>
      val href = model.pkFields match {
        case Nil => ""
        case fields =>
          val args = fields.map(f => s"model.${f.propertyName}").mkString(", ")
          s"""@${model.routesClass}.view($args)"""
      }
      if (model.pkFields.exists(pkField => pkField.propertyName == c.propertyName)) {
        file.add(s"""<td><a href="$href" class="theme-text">@model.${c.propertyName}</a></td>""")
      } else {
        model.foreignKeys.find(_.references.forall(_.source == c.columnName)) match {
          case Some(fk) if config.getModelOpt(fk.targetTable).isDefined =>
            file.add("<td>", 1)
            val tgt = config.getModel(fk.targetTable)
            if (!tgt.pkFields.forall(f => fk.references.map(_.target).contains(f.columnName))) {
              throw new IllegalStateException(s"FK [$fk] does not match PK [${tgt.pkFields.map(_.columnName).mkString(", ")}]...")
            }

            file.add(s"@model.${c.propertyName}")
            if (c.notNull) {
              file.add(s"""<a class="theme-text" href="@${tgt.routesClass}.view(model.${c.propertyName})">""", 1)
              file.add(tgt.iconHtml)
              file.add("</a>", -1)
            } else {
              file.add(s"@model.${c.propertyName}.map { v =>", 1)
              file.add(s"""<a class="theme-text" href="@${tgt.routesClass}.view(v)">""", 1)
              file.add(tgt.iconHtml)
              file.add("</a>", -1)
              file.add("}", -1)
            }
            file.add("</td>", -1)
          case _ => file.add(s"<td>@model.${c.propertyName}</td>")
        }
      }
    }
    file.add("</tr>", -1)
    file
  }
}

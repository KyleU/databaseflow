package com.databaseflow.services.scalaexport.graphql

import sangria.ast.{ListType, NamedType, NotNullType, Type}

object GraphQLInputTranslations {
  def scalaType(typ: Type): String = typ match {
    case NotNullType(x, _) => scalaType(x)
    case ListType(x, _) => s"Seq[${scalaType(x)}]"
    case NamedType(name, _) => name match {
      case "FilterInput" => "Filter"
      case "OrderByInput" => "OrderBy"
      case _ => name
    }
    case _ => typ.toString
  }

  def scalaImport(providedPrefix: String, t: Type): Option[(String, String)] = t match {
    case NotNullType(x, _) => scalaImport(providedPrefix, x)
    case ListType(x, _) => scalaImport(providedPrefix, x)
    case _ => t.namedType.renderCompact match {
      case "UUID" => Some("java.util" -> "UUID")
      case "FilterInput" => Some((providedPrefix + "models.result.filter") -> "Filter")
      case "OrderByInput" => Some((providedPrefix + "models.result.orderBy") -> "OrderBy")
      case _ => None
    }
  }

  def defaultVal(typ: String) = typ match {
    case _ if typ.startsWith("Seq[") => " = Nil"
    case _ if typ.startsWith("Option[") => " = None"
    case _ => ""
  }
}

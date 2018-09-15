package com.databaseflow.models.scalaexport.db.config

import com.databaseflow.models.scalaexport.db.{ExportEnum, ExportModel}
import util.JsonSerializers._

object ExportConfiguration {
  implicit val jsonEncoder: Encoder[ExportConfiguration] = deriveEncoder
  implicit val jsonDecoder: Decoder[ExportConfiguration] = deriveDecoder
}

case class ExportConfiguration(
    key: String,
    projectId: String,
    projectTitle: String,
    flags: Set[String],
    enums: Seq[ExportEnum],
    models: Seq[ExportModel],
    source: String = "boilerplay",
    projectLocation: Option[String] = None,
    providedPackage: Option[String] = None,
    corePackage: Option[String] = None,
    coreLocation: Option[String] = None,
    wikiLocation: Option[String] = None,
    modelLocationOverride: Option[String] = None,
    thriftLocationOverride: Option[String] = None
) {
  def getModel(k: String) = getModelOpt(k).getOrElse(throw new IllegalStateException(s"No model available with name [$k]."))
  def getModelOpt(k: String) = getModelOptWithIgnored(k).filterNot(_.ignored)
  def getModelOptWithIgnored(k: String) = models.find(m => m.tableName == k || m.propertyName == k || m.className == k)

  val providedPrefix = providedPackage.map(_ + ".").getOrElse("")
  val pkgPrefix = corePackage.getOrElse("").split('.').filter(_.nonEmpty).toList
  val corePrefix = corePackage.map(_ + ".").getOrElse("")

  lazy val packages = {
    if (models.exists(_.pkg.isEmpty)) {
      val roots = models.filter(_.pkg.isEmpty).map(_.className)
      throw new IllegalStateException(s"Each model must have a package defined, however [${roots.mkString(", ")}] do not specify one.")
    }

    val packageModels = models.filter(_.pkg.nonEmpty).filterNot(_.provided)
    val modelPackages = packageModels.groupBy(_.pkg.head).toSeq.filter(_._2.nonEmpty).sortBy(_._1)

    val packageEnums = enums.filter(_.pkg.nonEmpty).filterNot(_.ignored)
    val enumPackages = packageEnums.groupBy(_.pkg.head).toSeq.filter(_._2.nonEmpty).sortBy(_._1)

    val packages = (enumPackages.map(_._1) ++ modelPackages.map(_._1)).distinct

    packages.map { p =>
      val ms = modelPackages.filter(_._1 == p).flatMap(_._2)
      val es = enumPackages.filter(_._1 == p).flatMap(_._2)

      val solo = ms.size == 1 && es.isEmpty
      (p, ms, es, solo)
    }
  }
}

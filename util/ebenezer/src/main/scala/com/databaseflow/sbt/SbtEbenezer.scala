package com.databaseflow.sbt

import sbt._
import sbt.Keys._
import com.twitter.scrooge.ScroogeSBT.autoImport._
import com.databaseflow.services.scalaexport.ExportFiles

object SbtEbenezer extends AutoPlugin {
  override def requires = com.twitter.scrooge.ScroogeSBT

  object autoImport {
    val ebenezer = TaskKey[Seq[File]](
      "ebenezer",
      "Generate better code from Thrift files using Database Flow"
    )

    val depPrefix = settingKey[String]("The package prefix for referenced classes.")
  }

  import autoImport._

  val ebenezerSettings: Seq[Setting[_]] = Seq(
    autoImport.depPrefix := "",
    ebenezer := {
      val startMs = System.currentTimeMillis

      val streamValue = streams.value
      def log(s: String) = streamValue.log.debug(s)

      val outputFolder = (sourceManaged in Compile).value / "dbf"
      val thriftSources = scroogeThriftSources.value
      val thriftIncludes = scroogeThriftIncludes.value
      val thriftNamespaceMap = scroogeThriftNamespaceMap.value

      if ( /* scroogeIsDirty.value && */ thriftSources.nonEmpty) {
        log(s"Database Flow code generation is running for [${thriftSources.size}] thrift sources, saving result to [${outputFolder.getPath}]...")
        log(s"Processing [${thriftSources.mkString(", ")}]")
        val loc = IO.createTemporaryDirectory
        ExportFiles.rootLocation = outputFolder.getAbsolutePath

        val result = compile(streamValue.log, outputFolder, thriftSources.toSet, thriftIncludes.toSet, thriftNamespaceMap, depPrefix.value)
        log(s"Code generation completed in [${System.currentTimeMillis - startMs}ms]")
        log(s"Exported:")
        result.foreach(f => log("  - " + f))
        result
      } else {
        log("Database Flow code generation up to date.")
        Nil
      }
    },
    sourceGenerators += ebenezer.taskValue
  )

  override lazy val projectSettings = inConfig(Test)(ebenezerSettings) ++ inConfig(Compile)(ebenezerSettings)

  private[this] def compile(
    log: Logger,
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    depPrefix: String
  ) = {
    outputDir.mkdirs()

    val result = thriftFiles.toIndexedSeq.map { f =>
      com.databaseflow.services.scalaexport.ScalaExport.exportThrift(
        input = Some(f.getAbsolutePath),
        output = Some(outputDir.getAbsolutePath),
        flags = Set("inplace", "simple", "enumObj"),
        configLocation = f.getParentFile.getPath,
        depPrefix = depPrefix
      )
    }

    result.flatMap(_._2.map(_._1)).map(outputDir / _).distinct
  }
}

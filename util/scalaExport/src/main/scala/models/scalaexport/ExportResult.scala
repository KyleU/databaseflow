package models.scalaexport

case class ExportResult(id: String, models: Seq[(Seq[String], String)], files: Seq[OutputFile]) {
  private[this] val startTime = System.currentTimeMillis
  private[this] val logs = collection.mutable.ArrayBuffer.empty[(Int, String)]
  def log(msg: String) = logs += ((System.currentTimeMillis - startTime).toInt -> msg)
  val getLogs: Seq[(Int, String)] = logs

  def getMarkers(key: String) = files.flatMap(_.markersFor(key)).distinct
}

package models.engine

import models.engine.rdbms._

object DatabaseEngine {
  val rdbmsEngines = Seq(DB2, H2, Informix, MySQL, Oracle, PostgreSQL, SQLite, SQLServer)
  val all = rdbmsEngines
  private[this] val enginesById = all.map(x => x.id -> x).toMap
  def get(id: String) = enginesById.getOrElse(id, throw new IllegalArgumentException(s"No database engine registered as [$id]."))
}

abstract class DatabaseEngine(
    val id: String,
    val name: String,
    val driverClass: String,
    val defaultPort: Option[Int] = None,
    val exampleUrl: String
) {
  def builtInFunctions: Seq[String] = Nil
  def columnTypes: Seq[String] = Nil

  def leftQuoteIdentifier = "\""
  def rightQuoteIdentifier = "\""

  def explain: Option[(String) => String] = None
  def analyze: Option[(String) => String] = None

  def transactionsSupported = true

  def url(host: Option[String], port: Option[Int], dbName: Option[String], extra: Option[String]) = dbName match {
    case Some(d) => throw new IllegalStateException(s"Cannot form url for provided [$host:$port/$dbName:$extra].")
    case None => exampleUrl
  }

  override def toString = id
}

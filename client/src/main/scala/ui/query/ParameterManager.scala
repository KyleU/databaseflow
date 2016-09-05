package ui.query

import java.util.UUID

import models.template.query.QueryParametersTemplate
import org.scalajs.jquery.{JQuery, jQuery => $}
import utils.TemplateUtils

object ParameterManager {
  private[this] var activeParams = Map.empty[UUID, Seq[(String, String, String)]]

  private[this] def set(queryId: UUID, v: Seq[(String, String, String)]) = {
    utils.Logging.info(s"Setting values for [$queryId]: $v")
    activeParams += queryId -> v
  }

  def setValues(queryId: UUID, paramValues: Map[String, String]) = {
    val newParams = activeParams.get(queryId) match {
      case Some(params) => params.map { v =>
        (v._1, v._2, paramValues.getOrElse(v._1, v._3))
      } ++ paramValues.toSeq.filterNot(x => params.exists(_._1 == x._1)).map(x => (x._1, "string", x._2))
      case None => paramValues.toSeq.map(x => (x._1, "string", x._2))
    }
    set(queryId, newParams)
  }

  def onChange(queryId: UUID, sql: String, forceRefresh: Boolean = false) = {
    //utils.Logging.info(s"onChange(queryId: $queryId, sql: $sql, paramValues: $paramValues)")
    val keys = getKeys(sql)
    val hasChanged = forceRefresh || (activeParams.get(queryId) match {
      case Some(params) => (params.size != keys.size) || (!params.zip(keys).forall(x => x._1._1 == x._2._1 && x._1._2 == x._2._2))
      case None => throw new IllegalStateException(s"Cache not initialized for query [$queryId].")
    })
    if (hasChanged) {
      val panel = $(s"#panel-$queryId .sql-parameters")
      render(queryId, keys, panel)
      TemplateUtils.changeHandler($("input", panel), jq => {
        val k = jq.data("key").toString
        val t = jq.data("t").toString
        val v = jq.value().toString
        val orig = activeParams(queryId)
        val merged = orig.filterNot(_._1 == k) :+ ((k, t, v))
        utils.Logging.info(s"Orig: $orig / Merged: $merged")
        set(queryId, merged)
        val mergedSql = merge(sql, merged.map(x => x._1 -> x._3).toMap)
        QueryCheckManager.check(queryId, mergedSql)
      })
    }
  }

  def getParamsOpt(queryId: UUID) = activeParams.get(queryId).map { x =>
    utils.Logging.info(s"Returning params [$x].")
    x.map(r => r._1 -> r._3).toMap
  }

  def getParams(sql: String, queryId: UUID) = sql -> getParamsOpt(queryId).getOrElse(Map.empty)

  def remove(queryId: UUID) = activeParams = activeParams - queryId

  def merge(sql: String, params: Map[String, String]) = {
    var merged = sql
    params.foreach { param =>
      if (param._2.trim.nonEmpty) {
        var idx = Math.max(merged.indexOf("{" + param._1 + ":"), merged.indexOf("{" + param._1 + "}"))
        while (idx > -1) {
          val end = merged.indexOf('}', idx) + 1
          merged = merged.replaceAllLiterally(merged.substring(idx, end), param._2)
          idx = Math.max(merged.indexOf("{" + param._1 + ":"), merged.indexOf("{" + param._1 + "}"))
        }
      }
    }
    merged
  }

  private[this] def getKeys(sql: String) = {
    var startIndex = -1
    sql.zipWithIndex.foldLeft(Seq.empty[(String, String)])((x, y) => y match {
      case ('{', idx) =>
        startIndex = idx
        x
      case ('}', idx) if idx == (startIndex + 1) => x
      case ('}', idx) =>
        val v = sql.substring(startIndex + 1, idx)
        val ret = v.indexOf(':') match {
          case -1 => v -> "string"
          case i =>
            val split = v.split(':')
            split.headOption.getOrElse(throw new IllegalStateException()).trim -> split.tail.mkString(":").trim
        }
        if (!x.exists(_._1 == ret._1)) {
          x :+ ret
        } else {
          x
        }
      case _ => x
    })
  }

  private[this] def render(queryId: UUID, keys: Seq[(String, String)], panel: JQuery) = {
    //utils.Logging.info("Render Keys: " + keys.mkString(", "))
    if (panel.length != 1) { throw new IllegalStateException(s"Encountered [${panel.length}] parameter panels.") }
    if (keys.isEmpty) {
      set(queryId, Nil)
      panel.hide()
    } else {
      val params = activeParams(queryId)
      val values = keys.map(k => (k._1, k._2, params.find(_._1 == k._1).map(_._3).getOrElse("")))
      set(queryId, values)
      panel.html(QueryParametersTemplate.forValues(queryId, values).toString)
      panel.show()
    }
  }
}
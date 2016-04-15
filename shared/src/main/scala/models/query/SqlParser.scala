package models.query

object SqlParser {
  def split(sql: String, delimiter: Char = ';') = {
    val len = sql.length
    var startIndex = 0
    var currentIndex = 0
    var ret = Seq.empty[String]

    while (currentIndex < len) {
      sql(currentIndex) match {
        case x if x == '"' || x == '\'' || x == '`' => sql.indexOf(x.toInt, currentIndex + 1) match {
          case -1 => throw new IllegalStateException(s"Unclosed [$x] string literal.")
          case idx => currentIndex = idx + 1
        }
        case x if x == '[' => sql.indexOf(']', currentIndex + 1) match {
          case -1 => currentIndex = sql.length
          case idx => currentIndex = idx + 1
        }
        case x if x == '/' => if (sql.length > currentIndex + 1 && sql(currentIndex + 1) == '*') {
          sql.indexOf("*/", currentIndex + 2) match {
            case -1 => currentIndex = sql.length
            case idx => currentIndex = idx + 2
          }
        } else {
          currentIndex += 1
        }
        case x if x == '-' => if (sql.length > currentIndex + 1 && sql(currentIndex + 1) == '-') {
          sql.indexOf('\n', currentIndex + 1) match {
            case -1 => currentIndex = sql.length
            case idx => currentIndex = idx + 1
          }
        } else {
          currentIndex += 1
        }
        case x if x == '#' => sql.indexOf('\n', currentIndex + 1) match {
          case -1 => currentIndex = sql.length
          case idx => currentIndex = idx + 1
        }
        case d if d == delimiter =>
          val statement = sql.substring(startIndex, currentIndex)
          ret = ret :+ statement
          currentIndex += 1
          startIndex = currentIndex

        case _ => currentIndex += 1
      }
    }

    ret = ret :+ sql.substring(startIndex)

    ret.map(_.trim).filter(_.nonEmpty)
  }
}
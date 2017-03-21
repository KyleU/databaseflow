package utils

object NumberUtils {
  def withCommas(i: Int): String = i.toString.reverse.grouped(3).mkString(",").reverse
  //def withCommas(n: Numeric[_]) = n.toString.reverse.grouped(3).mkString(",").reverse
  def withCommas(l: Long): String = l.toString.reverse.grouped(3).mkString(",").reverse
  def withCommas(d: Double): String = d.toString.split('.').toList match {
    case h :: t :: Nil => withCommas(h.toLong) + "." + t
    case h :: Nil => withCommas(h.toLong)
    case _ => d.toString
  }

  def toWords(i: Int, properCase: Boolean = false) = {
    val ret = i match {
      case 0 => "zero"
      case 1 => "one"
      case 2 => "two"
      case 3 => "three"
      case 4 => "four"
      case 5 => "five"
      case 6 => "six"
      case 7 => "seven"
      case 8 => "eight"
      case 9 => "nine"
      case 10 => "ten"
      case 11 => "eleven"
      case 12 => "twelve"
      case _ => i.toString
    }
    if (properCase) {
      ret.head.toUpper + ret.tail
    } else {
      ret
    }
  }
}

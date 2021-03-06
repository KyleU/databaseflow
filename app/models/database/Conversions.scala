package models.database

import java.sql.Timestamp

import org.joda.time.{LocalDateTime, LocalDate, DateTime, ReadableInstant}
import util.DateUtils

object Conversions {
  @SuppressWarnings(Array("MethodReturningAny"))
  def convert(x: AnyRef): AnyRef = x match {
    case num: BigDecimal => num.underlying()
    case num: BigInt => BigDecimal(num).underlying()

    // Convert Joda times to UTC.
    case ts: ReadableInstant => new Timestamp(new DateTime(ts.getMillis, ts.getZone).toDateTimeISO.getMillis)
    case d: LocalDate => new java.sql.Date(d.toDate.getTime)
    case d: LocalDateTime => new java.sql.Date(DateUtils.toMillis(d))

    // Pass everything else through.
    case _ => x
  }
}

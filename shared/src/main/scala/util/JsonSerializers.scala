package util

import java.time.{LocalDate, LocalDateTime, LocalTime}

import io.circe.generic.extras
import io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.encoding.ConfiguredObjectEncoder
import io.circe.java8.time._
import shapeless.Lazy

import scala.language.implicitConversions

object JsonSerializers {
  type Decoder[A] = io.circe.Decoder[A]
  type Encoder[A] = io.circe.Encoder[A]

  type Json = io.circe.Json

  implicit def encodeLocalDateTime: Encoder[LocalDateTime] = io.circe.java8.time.encodeLocalDateTimeDefault
  implicit def encodeLocalDate: Encoder[LocalDate] = io.circe.java8.time.encodeLocalDateDefault
  implicit def encodeLocalTime: Encoder[LocalTime] = io.circe.java8.time.encodeLocalTimeDefault
  implicit def decodeLocalDateTime: Decoder[LocalDateTime] = io.circe.java8.time.decodeLocalDateTimeDefault
  implicit def decodeLocalDate: Decoder[LocalDate] = io.circe.java8.time.decodeLocalDateDefault
  implicit def decodeLocalTime: Decoder[LocalTime] = io.circe.java8.time.decodeLocalTimeDefault

  implicit val circeConfiguration: extras.Configuration = extras.Configuration.default.withDefaults

  def deriveDecoder[A](implicit decode: Lazy[ConfiguredDecoder[A]]) = io.circe.generic.extras.semiauto.deriveDecoder[A]
  def deriveEncoder[A](implicit decode: Lazy[ConfiguredObjectEncoder[A]]) = io.circe.generic.extras.semiauto.deriveEncoder[A]
  def deriveFor[A](implicit decode: Lazy[ConfiguredDecoder[A]]) = io.circe.generic.extras.semiauto.deriveFor[A]

  implicit def encoderOps[A](a: A): io.circe.syntax.EncoderOps[A] = io.circe.syntax.EncoderOps[A](a)
  def parseJson(s: String) = io.circe.parser.parse(s)
  def decodeJson[A](s: String)(implicit decoder: Decoder[A]) = io.circe.parser.decode[A](s)
}

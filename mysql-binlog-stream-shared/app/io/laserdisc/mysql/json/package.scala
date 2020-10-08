package io.laserdisc.mysql

import io.circe._
import io.circe.parser._

package object json {
  def flatHash(doc: String, removeKey: String = ""): Either[Exception, String] =
    flatten(doc) match {
      case Left(e) => Left(e)
      case Right(js) =>
        Right(
          if (removeKey == "")
            md5sum(js.mkString(","))
          else
            md5sum(js.filter(!_.startsWith(removeKey)).mkString(","))
        )
    }

  def flatten(doc: String): Either[Exception, List[String]] =
    parse(doc) match {
      case Left(e)     => Left(e)
      case Right(json) => Right(flatSort(json))
    }

  def flatSort(json: Json): List[String] = flatUnsorted(json).sorted

  def flatUnsorted(json: Json): List[String] =
    json.foldWith(
      new Json.Folder[List[String]] with (Json => List[String]) {
        def apply(v: Json): List[String] = v.foldWith(this)
        def onObject(v: JsonObject) =
          v.toList.flatMap { case (k, v) =>
            List(k.trim ++ ":" ++ v.foldWith(this).mkString(","))
          }
        def onArray(v: Vector[Json]) =
          v.flatMap(j => j.foldWith(this)).toList.sorted
        def onBoolean(v: Boolean)   = List(v.toString)
        def onNumber(v: JsonNumber) = List(v.toString)
        def onString(v: String)     = List(v.trim)
        def onNull                  = Nil
      }
    )

  def md5sum(s: String): String = {
    import java.math.BigInteger
    import java.security.MessageDigest
    val md     = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    bigInt.toString(16)
  }
}

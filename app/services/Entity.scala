package services

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OFormat}

case class Entity(name:String, shortUrl:String, longUrl:String, id:Long = 0L)
object Entity {
  def tuple2E(name:String,shortUrl:String,longUrl:String): Entity = Entity(name,shortUrl,longUrl)
  def e2Tuple(e:Entity): (String, String, String) = (e.name, e.shortUrl, e.longUrl)
  implicit val entityFormat: OFormat[Entity] =
    ((JsPath \ "name").format[String] and
      (JsPath \ "shortUrl").format[String] and
      (JsPath \ "longUrl").format[String])(tuple2E, e2Tuple)
}

package services

import java.time.LocalDateTime

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Entity(keyword:String,
                  redirectURL:String,
                  note:Option[String],
                  updateTime: LocalDateTime = LocalDateTime.now(), id:Long = 0L)

object Entity {
  def tuple2E(keyword:String,redirectURL:String,note:Option[String]): Entity = Entity(keyword,redirectURL,note)
  implicit val entityRead: Reads[Entity] =
    ((JsPath \ "keyword").read[String] and
    (JsPath \ "redirectURL").read[String] and
    (JsPath \ "note").readNullable[String])(tuple2E _)
  implicit val entityWrite: Writes[Entity] = (o: Entity) => Json.obj(
    "keyword" -> o.keyword,
    "redirectURL" -> o.redirectURL,
    "note" -> o.note,
    "updateTime" -> o.updateTime,
    "id" -> o.id
  )
}

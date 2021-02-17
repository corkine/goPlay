package services

import java.time.LocalDateTime

import play.api.libs.functional.syntax._
import play.api.libs.json._

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

sealed trait EntityAction
case object Visit extends EntityAction
case object Edit extends EntityAction
case object Create extends EntityAction
case object Delete extends EntityAction
object EntityAction {
  implicit val actionWrite: Writes[EntityAction] =
    (o: EntityAction) => JsString(o.getClass.getSimpleName.replace("$",""))
}

case class EntityLog(entityId:Option[Long], visitorIP:String,
                     actionType:EntityAction,
                     actionTime:LocalDateTime = LocalDateTime.now(), logId:Long = 0L)
object EntityLog {
  implicit val entityLogFormat: OWrites[EntityLog] = Json.writes[EntityLog]
}
case class RichEntityLog(entityLog: EntityLog, ipInfo: String, keyword: String, url: String)
object RichEntityLog {
  implicit val entityLogRichFormat: OWrites[RichEntityLog] = Json.writes[RichEntityLog]
}

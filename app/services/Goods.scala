package services

import java.time.LocalDateTime

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json, Reads, Writes}

case class Goods(name:String,
                category:Long,
                location:Long,
                attached:Option[String] = None,
                priority:Int = 0,
                notes:Option[String] = None,
                picture:Option[Array[Byte]] = None,
                update:LocalDateTime = LocalDateTime.now(),
                uuid:String = "")

object Goods {
  def tuple2G(name:String,category:Long,location:Long,attached:Option[String],
             priority:Int,notes:Option[String],update:LocalDateTime,
             uuid:String): Goods = Goods(name,category,location,attached,priority,
    notes,None,update,uuid)
  implicit val goodsRead: Reads[Goods] =
    ((JsPath \ "name").read[String] and
      (JsPath \ "category").read[Long] and
      (JsPath \ "location").read[Long] and
      (JsPath \ "attached").readNullable[String] and
      (JsPath \ "priority").read[Int] and
      (JsPath \ "notes").readNullable[String] and
      (JsPath \ "update").read[LocalDateTime] and
      (JsPath \ "uuid").read[String])(tuple2G _)
  implicit val goodsWrite: Writes[Goods] = (o: Goods) => Json.obj(
    "name" -> o.name,
    "categoryId" -> o.category,
    "locationId" -> o.location,
    "attached" -> o.attached,
    "priority" -> o.priority,
    "notes" -> o.notes,
    "update" -> o.update,
    "uuid" -> o.uuid
  )
}

case class Category(name:String, note:Option[String], id:Long)
object Category {
  implicit val catFormat: Format[Category] =
    ((JsPath \ "name").format[String] and
      (JsPath \ "note").formatNullable[String] and
      (JsPath \ "id").format[Long])(Category.apply, unlift(Category.unapply))
}

case class Location(name:String, note:Option[String], id:Long)
object Location {
  implicit val locFormat: Format[Location] =
    ((JsPath \ "name").format[String] and
      (JsPath \ "note").formatNullable[String] and
      (JsPath \ "id").format[Long])(Location.apply, unlift(Location.unapply))
}
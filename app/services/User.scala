package services

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

case class User(username:String, password:String, userType: UserType = Common, id:Long = 0L)

trait UserType
object UserType {
  implicit def pathBinder: QueryStringBindable[UserType] = new QueryStringBindable[UserType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UserType]] = {
      params.get("userType").map(i => i.head.toUpperCase() match {
        case "ADMIN" => Right(Admin)
        case "COMMON" => Right(Common)
        case _ => Left(s"Can't parse $key - $i to UserType.")
      })
    }
    override def unbind(key: String, value: UserType): String = {
      value match {
        case Admin => "Admin"
        case Common => "Common"
      }
    }
  }
}
object Admin extends UserType
object Common extends UserType

object User {
  implicit val utFormats: Format[UserType] = new Format[UserType] {
    override def reads(json: JsValue): JsResult[UserType] = json.as[String].toUpperCase match {
      case "ADMIN" => JsSuccess(Admin)
      case "COMMON" => JsSuccess(Common)
      case _ => JsError("Can't read from userType Json Type.")
    }
    override def writes(o: UserType): JsValue = o match {
      case Admin => JsString("Admin")
      case Common => JsString("Common")
      case _ => JsString("Common")
    }
  }
  implicit val userFormats: Format[User] = (
    (JsPath \ "username").format[String] and
      (JsPath \ "password").format[String] and
      (JsPath \ "userType").format[UserType] and
      (JsPath \ "id").format[Long])(User.apply, unlift(User.unapply))
}

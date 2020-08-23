package controllers

import java.util.Base64

import javax.inject.{Inject, Singleton}
import play.api.libs.Codecs
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc:ControllerComponents, entityService: EntityService, userService:UserService)
                              (implicit ec:ExecutionContext)
  extends AbstractController(cc) {
  ////////////////////////////////// Public API //////////////////////////////////

  def index = Action { Ok(views.html.Introduction()) }

  def go(shortUrl:String) = Action.async {
    @inline def handleURL(str: String): String = {
      if (str.startsWith("http://") || str.startsWith("https://")) str
      else "http://" + str
    }
    entityService.find(shortUrl).map {
      case i if i.nonEmpty => Redirect(handleURL(i.head.redirectURL))
      case _ => NotFound
    }
  }

  def id(id:Long) = Action.async {
    entityService.id(id).map {
      case Some(value) => Redirect(routes.HomeController.go(value.keyword))
      case None => NotFound
    }
  }

  def schema(clazz:String): Action[AnyContent] = Action {
    val res = clazz match {
      case "entity" => entityService.schema
      case "user" => userService.schema
      case _ => "NOTHING"
    }
    Ok(Json.obj("schema" -> res))
  }

  ////////////////////////////////// User Private Controller Method //////////////////////////////////

  def info(id:Long): Action[AnyContent] = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.id(id).map {
          case Some(value) => Ok(Json.toJson(value))
          case None => NotFound
        }
    }
  }

  def addGet(keyword:String, redirectURL:String, note:Option[String]): Action[AnyContent] = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.add(Entity(keyword,redirectURL,note)).collect {
          case (conflict,_) if conflict =>
            Ok(Json.obj("status" -> 0, "entity" -> None,"message" -> "Create failed, existed"))
          case (_,entity) =>
            Ok(Json.obj("status" -> 1, "entity" -> entity,"message" -> s"Create 1 done."))
        }
    }
  }

  def addJSON: Action[AnyContent] = Action.async { req =>
    authUsers(req) map {
      case Right(value) => value
      case Left(_) =>
        req.body.asJson.map(_.validate[Entity]) match {
          case None => message("No Json Found")
          case Some(value) => value match {
            case JsError(errors) => message(s"Error $errors")
            case JsSuccess(e, _) =>
              Redirect(routes.HomeController.addGet(e.keyword,e.redirectURL,e.note))
          }
        }
    }
  }

  def token: Action[AnyContent] = Action.async { req =>
    @inline def getTokenWrapped(user: User):String = {
      val expTime = (System.currentTimeMillis() / 1000) + (24 * 60 * 60)
      val encryptedPassword = Codecs.sha1(user.password + ":" + expTime)
      Base64.getEncoder.encodeToString(s"${user.username}:$encryptedPassword:$expTime".getBytes)
    }
    authUsers(req) flatMap {
      case Right(_) => Future(Unauthorized(Json.obj("status" -> -1,
        "message" -> "Auth Failed", "token" -> None)).withHeaders(WWW_AUTHENTICATE -> "Basic"))
      case Left(value) => Future(Ok(Json.obj("status" -> 1,
        "message" -> "Auth successful.", "token" -> getTokenWrapped(value))))
    }
  }

  def checkKeyword(keyword:String): Action[AnyContent] = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) => entityService.check(keyword).map {
        case None => Ok(Json.obj("status" -> 0, "message" -> "haven't find this entity in db.", "entity" -> None))
        case Some(value) => Ok(Json.obj("status" -> 1, "message" -> "Done.", "entity" -> value))
      }
    }
  }

  ////////////////////////////////// Admin Private Controller Method //////////////////////////////////
  def list(limit:Int): Action[AnyContent] = Action.async { req =>
    authAdmin(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.list(limit).map { bean => Ok(Json.toJson(bean)) }
    }
  }

  def delete(id:Long): Action[AnyContent] = Action.async { req =>
    authAdmin(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.delete(id).map {
          case 1 => message("Delete 1 done.")
          case _ => message("Delete failed.")
        }
    }
  }

  def deleteKeyword(keyword:String): Action[AnyContent] = Action.async { req =>
    authAdmin(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) => entityService.deleteKeyword(keyword).map {
        case 0 => Ok(Json.obj("message" -> "Delete Failed.", "row" -> 0, "status" -> 0))
        case i if i != 0 => Ok(Json.obj("message" -> "Delete done.", "row" -> i, "status" -> 1))
      }
    }
  }

  def listUser = Action.async { req => authAdmin(req) flatMap {
      case Left(_) => userService.list.map { res => Ok(Json.toJson(res)) }
      case Right(value) => Future(value)
    }
  }

  def addUser(username:String, password:String, role:Option[UserType]) = Action.async { req =>
    authAdmin(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) => userService.add(username,password,role.getOrElse(Common)).map {
          case true => message("User add done.")
          case false => message("User add failed.")
        }
    }
  }

  def deleteUser(id:Long) = Action.async { req =>
    authAdmin(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        userService.delete(id) map {
          case 0 => message("delete failed.")
          case o => message(s"delete done with $o rows.")
        }
    }
  }

  ////////////////////////////////// Useful Tools //////////////////////////////////

  @inline def message(c:String): Result = Ok(Json.obj("message" -> c))

  def authenticatedAction(userType: Seq[UserType])(request: Request[AnyContent]): Future[Either[User,Result]] = {
    authenticatedInBase64(request) orElse
      authenticatedInQuery(request) orElse
        authenticatedInToken(request) match {
          case None => Future(Right(Unauthorized(Json.obj("message"-> "NOT_AUTHORIZED"))
            .withHeaders(WWW_AUTHENTICATE -> "Basic")))
          case Some(eit) => eit match {
            case Left(v) => Future(Right(v))
            case Right((u,p,useSHA1,exTime)) => userService.check(u,p,useSHA1,exTime).map {
              case Some(user) if userType.contains(user.userType) => Left(user)
              case None => Right(message("User token check failed."))
              case _ => Right(message("User not authorized, may not in super higher group."))
            }
          }
        }
  }

  def authenticatedInToken(request:Request[AnyContent]):Option[Either[Result,CheckData]] = {
    request.getQueryString("token").map { token =>
      try {
        new String(Base64.getDecoder.decode(token)).split(":",3) match {
          case Array(u,p,exTime) =>
            val time = exTime.toInt
            if (time < System.currentTimeMillis() / 1000)
              Left(message("Token expired."))
            else Right((u,p,true,time))
          case _ => Left(message("Invalid token authentication"))
        }
      } catch { case e: Throwable =>
        Left(message(s"Token parse error. ${e.getMessage}")) }
    }
  }

  type CheckData = (String,String,Boolean,Int)

  def authenticatedInQuery(request:Request[AnyContent]): Option[Either[Result,CheckData]] = {
    (for {
      user <- request.getQueryString("user")
      password <- request.getQueryString("password")
    } yield (user, password)) match {
      case None => None
      case Some((u,p)) =>
        Some(Right((u,p,false,0)))
    }
  }

  def authenticatedInBase64(request:Request[AnyContent]):Option[Either[Result,CheckData]] = {
    request.headers.get(HeaderNames.AUTHORIZATION).map { header =>
      val BasicHeader = "Basic (.*)".r
      header match {
        case BasicHeader(base64) =>
          try {
            new String(Base64.getDecoder.decode(base64)).split(":",2) match {
              case Array(u,p) => Right((u,p,false,0))
              case _ => Left(BadRequest("Invalid basic authentication"))
            }
          } catch {
            case e: Throwable => Left(BadRequest(s"Invalid basic authentication ${e.getMessage}"))
          }
      }
    }
  }

  def authAdmin: Request[AnyContent] => Future[Either[User, Result]] = authenticatedAction(Seq(Admin))

  def authUsers: Request[AnyContent] => Future[Either[User, Result]] = authenticatedAction(Seq(Admin,Common))
}

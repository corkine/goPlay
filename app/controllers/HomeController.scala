package controllers

import java.util.Base64

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request, Result}
import play.mvc.Http.HeaderNames
import services.{Admin, Common, Entity, EntityService, User, UserService, UserType}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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

  ////////////////////////////////// User Private Controller Method //////////////////////////////////

  def list(limit:Int) = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.list(limit).map { bean => Ok(Json.toJson(bean)) }
    }
  }

  def info(id:Long) = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.id(id).map {
          case Some(value) => Ok(Json.toJson(value))
          case None => NotFound
        }
    }
  }

  def addGet(keyword:String, redirectURL:String, note:Option[String]) = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.add(Entity(keyword,redirectURL,note)).collect {
          case (conflict,_) if conflict =>
            Ok(Json.obj("status" -> 0, "entity" -> "NONE","message" -> "Create failed, existed"))
          case (_,entity) =>
            Ok(Json.obj("status" -> 1, "entity" -> entity,"message" -> s"Create 1 done."))
        }
    }
  }

  def addJSON = Action.async { req =>
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

  def delete(id:Long) = Action.async { req =>
    authUsers(req) flatMap {
      case Right(value) => Future(value)
      case Left(_) =>
        entityService.delete(id).map {
          case 1 => message("Delete 1 done.")
          case _ => message("Delete failed.")
        }
    }
  }

  def schema(clazz:String) = Action {
    val res = clazz match {
      case "entity" => entityService.schema
      case "user" => userService.schema
      case _ => "NOTHING"
    }
    Ok(Json.obj("schema" -> res))
  }

  ////////////////////////////////// Admin Private Controller Method //////////////////////////////////

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

  @inline def message(c:String) = Ok(Json.obj("message" -> c))

  def authenticatedAction(userType: Seq[UserType])(request: Request[AnyContent]): Future[Either[User,Result]] = {
    authenticatedInQuery(request) orElse authenticatedInBase64(request) match {
      case None => Future(Right(Unauthorized(Json.obj("message"-> "NOT_AUTHORIZED"))
        .withHeaders(WWW_AUTHENTICATE -> "Basic")))
      case Some(eit) => eit match {
        case Left(v) => Future(Right(v))
        case Right((u,p)) => userService.check(u,p).map {
          case Some(user) if userType.contains(user.userType) => Left(user)
          case None => Right(message("User token check failed."))
          case _ => Right(message("User not authorized, may not in super higher group."))
        }
      }
    }
  }

  def authenticatedInQuery(request:Request[AnyContent]): Option[Either[Result,(String,String)]] = {
    (for {
      user <- request.getQueryString("user")
      password <- request.getQueryString("password")
    } yield (user, password)) match {
      case None => None
      case Some(value) => Some(Right(value))
    }
  }

  def authenticatedInBase64(request:Request[AnyContent]):Option[Either[Result,(String,String)]] = {
    request.headers.get(HeaderNames.AUTHORIZATION).map { header =>
      val BasicHeader = "Basic (.*)".r
      header match {
        case BasicHeader(base64) =>
          try {
            new String(Base64.getDecoder.decode(base64)).split(":",2) match {
              case Array(u,p) => Right(u -> p)
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

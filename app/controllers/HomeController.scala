package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.{Entity, EntityService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc:ControllerComponents, entityService: EntityService)
                              (implicit ec:ExecutionContext)
  extends AbstractController(cc) {
  def index = Action.async {
    entityService.list().map { bean =>
      Ok(Json.toJson(bean))
    }
  }

  def add(name:String, shortUrl:String, longUrl:String) = Action.async {
    entityService.add(Entity(name,shortUrl,longUrl)).collect {
      case Right((row,entity)) =>
        Ok(Json.obj("status" -> 1, "entity" -> entity,"message" -> s"Create $row done."))
      case Left(value) =>
        Ok(Json.obj("status" -> 0, "message" -> s"Error: $value"))
    }
  }

  def go(shortUrl:String) = Action.async {
    entityService.find(shortUrl).map {
      case i if i.nonEmpty => Redirect(i.head.longUrl)
      case _ => NotFound
    }
  }

  def schema = Action {
    Ok(Json.obj("schema" -> entityService.schema))
  }
}

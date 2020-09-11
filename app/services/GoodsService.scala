package services

import java.time.{LocalDate, LocalDateTime}

import controllers.Assets.Asset
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GoodsService @Inject()(protected val dbConfigProvider:DatabaseConfigProvider)
                             (implicit ec:ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private class EntityTable(tag:Tag) extends Table[Entity](tag, "Entities") {
    def id = column[Long]("id",O.PrimaryKey, O.AutoInc)
    def keyword = column[String]("keyword")
    def redirectURL = column[String]("redirectURL")
    def note = column[Option[String]]("note")
    def updateTime = column[LocalDateTime]("updateTime")
    override def * = (keyword,redirectURL,note,updateTime,id) <> ((Entity.apply _).tupled, Entity.unapply)
  }
  private val entity = TableQuery[EntityTable]

  def list(): Future[Seq[Entity]] = db.run {
    entity.result
  }

  def list(limit:Int): Future[Seq[Entity]] = db.run {
    entity.take(limit).result
  }

  def id(id:Long): Future[Option[Entity]] = db.run {
    entity.filter(_.id === id).result.headOption
  }

  def add(e: Entity): Future[(Boolean, Entity)] = db.run {
    entity.filter(_.keyword === e.keyword).result.headOption
  }.flatMap {
    case Some(existEntity) => Future(true -> existEntity)
    case None => db.run(
      (entity += e) >> entity.filter(_.keyword === e.keyword).result.head
    ).map(savedEntity => false -> savedEntity)
  }

  def delete(id:Long) = db.run {
    entity.filter(_.id === id).delete
  }

  def deleteKeyword(keyword:String): Future[Int] = db.run {
    entity.filter(_.keyword === keyword).delete
  }

  def find(keyword:String): Future[Seq[Entity]] = db.run {
    entity.filter(_.keyword === keyword).result
  }

  def check(keyword:String): Future[Option[Entity]] = db.run {
    entity.filter(_.keyword === keyword).result.headOption
  }

  def schema: String = entity.schema.createStatements.mkString("\n").replace("\"","'")
}

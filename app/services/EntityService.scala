package services

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityService @Inject()(protected val dbConfigProvider:DatabaseConfigProvider)
                             (implicit ec:ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private class EntityTable(tag:Tag) extends Table[Entity](tag, "Entity") {
    def id = column[Long]("id",O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def shortUrl = column[String]("shortUrl")
    def longUrl = column[String]("longUrl")
    override def * = (name,shortUrl,longUrl,id) <> ((Entity.apply _).tupled, Entity.unapply)
  }
  private val entity = TableQuery[EntityTable]

  def list(): Future[Seq[Entity]] = db.run {
    entity.result
  }

  def add(newEntity:Entity): Future[Either[String,(Int,Entity)]] =
    db.run(entity.filter(e => e.name === newEntity.name ||
      e.shortUrl === newEntity.shortUrl).exists.result).flatMap {
      case false => db.run(entity += newEntity).map(r => Right(r -> newEntity))
      case true => Future(Left("Conflict with name or shortUrl"))
    }

  def find(shortUrl:String): Future[Seq[Entity]] = db.run {
    entity.filter(_.shortUrl === shortUrl).result
  }

  def schema = entity.schema.createStatements.mkString("\n").replace("\"","'")
}

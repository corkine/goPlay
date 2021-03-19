package services

import org.lionsoul.ip2region.{DbConfig, DbSearcher, Util}
import play.api.Logger

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityService @Inject()(protected val dbConfigProvider:DatabaseConfigProvider)
                             (implicit ec:ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val dbSearcher = new DbSearcher(new DbConfig(),"public/ip2region.db")

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

  private implicit val autoActionMap: BaseColumnType[EntityAction] = MappedColumnType.base[EntityAction,String](a =>
    a.getClass.getSimpleName.replace("$",""), _.toLowerCase match {
      case "visit" => Visit
      case "edit" => Edit
      case "create" => Create
      case "delete" => Delete
      case _ => Visit
    })

  private class EntityLogTable(tag:Tag) extends Table[EntityLog](tag, "EntityLogs") {
    def logId = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def visitorIP = column[String]("IPAddress")
    def actionType = column[EntityAction]("action")
    def actionTime = column[LocalDateTime]("time")
    def entityId = column[Option[Long]]("entityId")
    def entityIds = foreignKey("entity_fk",entityId, entity)(_.id.?,
      onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.SetNull)
    override def * = (entityId, visitorIP,
      actionType, actionTime, logId) <> ((EntityLog.apply _).tupled, EntityLog.unapply)
  }
  private val entityLog = TableQuery[EntityLogTable]

  def list(): Future[Seq[Entity]] = db.run {
    entity.result
  }

  def list(limit:Int): Future[Seq[Entity]] = db.run {
    entity.sortBy(_.updateTime.desc).take(limit).result
  }

  def listLogs(limit:Int): Future[Seq[EntityLog]] = db.run {
    entityLog.sortBy(_.actionTime.desc).take(limit).result
  }

  def listLogsReadable(recentDay:Int,limit:Int,withIpResolve:Boolean): Future[Seq[RichEntityLog]] = db.run {
    (for {
      (entity, logs) <- entityLog.filter(e => e.actionTime >= LocalDateTime.now().minusDays(recentDay))
        .sortBy(_.actionTime.desc).take(limit) join entity on (_.entityId === _.id)
    } yield (entity, logs)).result
  }.map(_.map(a => RichEntityLog(a._1,if (withIpResolve) {
    if (Util.isIpAddress(a._1.visitorIP)) dbSearcher.memorySearch(a._1.visitorIP).getRegion
    else "Can't parse IP Address"
  } else "", a._2.keyword,a._2.redirectURL)))


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

  def delete(id:Long): Future[Int] = db.run {
    entity.filter(_.id === id).delete
  }

  def deleteKeyword(keyword:String): Future[Int] = db.run {
    entity.filter(_.keyword === keyword).delete
  }

  /*def add(newEntity:Entity): Future[Either[String,(Int,Entity)]] =
    db.run(entity.filter(e => e.name === newEntity.name ||
      e.keyword === newEntity.keyword).exists.result).flatMap {
      case false => db.run(entity += newEntity).map(r => Right(r -> newEntity))
      case true => Future(Left("Conflict with name or shortUrl"))
    }*/

  def find(keyword:String): Future[Seq[Entity]] = db.run {
    entity.filter(_.keyword === keyword).result
  }

  def find(keyword:String, fromIP:String): Future[Entity] = db.run {
    entity.filter(_.keyword === keyword).result.headOption.flatMap ({
      case Some(e) => (entityLog += new EntityLog(Some(e.id),fromIP,Visit)).map(_ => e)
      case _ => DBIO.successful(null)
    }:Option[Entity] => DBIOAction[Entity,NoStream,Effect.All])
  }

  def addFindLog(log: EntityLog): Future[Int] = db.run {
    entityLog += log
  }

  def listFindEntityWithLogs(keyword:String, logLimits:Int = 100): Future[(Entity,Seq[EntityLog])] = db.run {
    entity.filter(_.keyword === keyword).result.headOption.flatMap ({
      case Some(e) =>
        entityLog.filter(_.entityId === e.id).sortBy(_.actionTime.desc).take(logLimits)
          .result.flatMap(logs => DBIO.successful((e, logs)))
      case _ => DBIO.successful(null)
    }:Option[Entity] => DBIOAction[(Entity,Seq[EntityLog]),NoStream,Effect.All])
  }

  def setSecret(shortUrl:String, secret:Option[String]): Future[Int] = db.run {
    entity.filter(_.keyword === shortUrl).map(_.note).update(secret.map(sec => s"MS${sec}MS"))
  }

  def listFindLog(keyword:String): Future[Seq[EntityLog]] = db.run {
    entity.filter(_.keyword === keyword).result.headOption.flatMap ({
      case Some(Entity(_, _, _, _, id)) =>
        entityLog.filter(_.entityId === id).result
      case _ => DBIO.successful(Seq[EntityLog]())
    }:Option[Entity] => DBIOAction[Seq[EntityLog],NoStream,Effect.All])
  }

  def listFindLogById(id:Long): Future[Seq[EntityLog]] = db.run {
    entityLog.filter(_.entityId === id).result
  }

  def check(keyword:String): Future[Option[Entity]] = db.run {
    entity.filter(_.keyword === keyword).result.headOption
  }

  def search(keyword:String): Future[Seq[Entity]] = db.run {
    entity.filter(_.keyword like s"%$keyword%").sortBy(_.updateTime.desc).result
  }

  def schema: String = entity.schema.createStatements.mkString("\n").replace("\"","'") + "\n\n\n\n" +
    entityLog.schema.createStatements.mkString("\n").replace("\"","'")
}

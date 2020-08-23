package services

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.Codecs
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(protected val dbConfigProvider:DatabaseConfigProvider)
                             (implicit ec:ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private implicit val utType: BaseColumnType[UserType] = MappedColumnType.base[UserType,String](
    {
      case Admin => "Admin"
      case Common => "Common"
      case _ => "Common"
    },
    s => s.toUpperCase() match {
      case "ADMIN" => Admin
      case "COMMON" => Common
      case _ => Common
    }
  )

  private class Users(tag:Tag) extends Table[User](tag,"Users") {
    def username = column[String]("username")
    def password = column[String]("password")
    def userType = column[UserType]("userType")
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    override def * = (username,password,userType,id) <> ((User.apply _).tupled, User.unapply)
  }

  private val users = TableQuery[Users]

  def check(username:String, tokenOrPassword:String, useSHA1:Boolean, exTime:Int): Future[Option[User]] = {
    db.run(users.filter(i => i.username === username).result.headOption) map {
      case None => None
      case Some(u) =>
        if (useSHA1) {
          if (Codecs.sha1(u.password + ":" + exTime) == tokenOrPassword) Some(u)
          else None
        } else {
          if (tokenOrPassword == u.password) Some(u) else None
        }
    }
  }

  def schema: String = users.schema.createStatements.mkString(" ")

  def add(username:String,password:String,role:UserType): Future[Boolean] =
    for {
      userExist <- db.run(users.filter(_.username === username).exists.result)
      _ <- if (!userExist) db.run(users += User(username, password, role))
           else Future.successful(0)
    } yield !userExist

    /*def add2(username:String,password:String): Future[Boolean] =
    (for {
      userExist <- db.run(users.filter(_.username === username).exists.result)
      _ <- db.run(users += User(username, password)) if !userExist
    } yield !userExist).recover {
      case _ => false
    }*/

  def list: Future[Seq[User]] = db.run(users.result)

  def delete(id:Long): Future[Int] = db.run(users.filter(_.id === id).delete)

}

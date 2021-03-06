package models

import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.WrappingQuery

import java.util.Date

import andon.utils._
import andon.utils.DateUtil.dateTypeMapper

case class Article(
  id: Long,
  createAccountId: Int,
  updateAccountId: Int,
  title: String,
  text: String,
  createDate: Date,
  updateDate: Date,
  articleType: ArticleType,
  genre: String,
  optAuthor: Option[String],
  optDate: Option[String],
  editable: Boolean
)

object Articles extends Table[Article]("ARTICLES") {

  val db = DB.db

  def id = column[Long]("ID", O.NotNull, O.PrimaryKey, O.AutoInc)
  def createAccountId = column[Int]("CREATE_ACCOUNT_ID", O.NotNull)
  def updateAccountId = column[Int]("UPDATE_ACCOUNT_ID", O.NotNull)
  def title = column[String]("TITLE", O.NotNull)
  def text = column[String]("TEXT", O.NotNull)
  def createDate = column[Date]("CREATE_DATE", O.NotNull)
  def updateDate = column[Date]("UPDATE_DATE", O.NotNull)
  def articleType = column[ArticleType]("ARTICLE_TYPE", O.NotNull)
  def genre = column[String]("GENRE", O.NotNull)
  def optAuthor = column[Option[String]]("OPT_AUTHOR")
  def optDate = column[Option[String]]("OPT_DATE")
  def editable = column[Boolean]("EDITABLE")

  def * = id ~ createAccountId ~ updateAccountId ~ title ~ text ~
    createDate ~ updateDate ~ articleType ~ genre ~ optAuthor ~ optDate ~ editable <>
    (Article.apply _, Article.unapply _)

  def ins = createAccountId ~ updateAccountId ~ title ~ text ~
    createDate ~ updateDate ~ articleType ~ genre ~ optAuthor ~ optDate ~ editable returning id

  val query = Query(Articles)

  val desc = query.sortBy(_.id.desc)
  val howtos = query.where(_.articleType === (Howto: ArticleType))

  def dateSort(q: Query[Articles.type, Article]) =
    q.sortBy(_.updateDate.desc).sortBy(_.optDate.desc.nullsFirst)

  def findById(id: Long) = db.withSession { implicit session: Session =>
    query.where(_.id === id).firstOption
  }

  def findByCreateAccountId(aId: Int) = db.withSession { implicit session: Session =>
    desc.where(_.createAccountId === aId).list
  }

  def findByType(t: ArticleType) = db.withSession { implicit session: Session =>
    desc.where(_.articleType === t).list
  }

  // page = 0, 1, 2 ...
  def findInfoByPage(page: Int, num: Int) = db.withSession { implicit session: Session =>
    desc.where(_.articleType === (Info: ArticleType)).drop(page * num).take(num).list
  }

  def findByGenre(g: String) = db.withSession { implicit session: Session =>
    desc.where(_.genre === g).list
  }

  def findHowtoByGenre(g: String) = db.withSession { implicit session: Session =>
    dateSort(howtos.where(_.genre === g)).list
  }

  def findDateSortedHowto = db.withSession { implicit session: Session =>
    dateSort(howtos).list
  }

  def findByWriterEditable(aId: Int) = db.withSession { implicit session: Session =>
    dateSort(howtos.where(a => a.createAccountId === aId || a.editable === true)).list
  }

  def findTitleById(id: Long) = db.withSession { implicit session: Session =>
    query.where(_.id === id).firstOption.map { article =>
      article.title
    }.getOrElse("?")
  }

  def all = db.withSession { implicit session: Session =>
    desc.list
  }

  // return = 0, 1, 2 ...
  def countPageByType(t: ArticleType, num: Int) = db.withSession { implicit session: Session =>
    val count = Query(query.where(_.articleType === t).length).first
    if (count == 0) {
      0
    } else if (count % num == 0) {
      count / num - 1
    } else {
      count / num
    }
  }

  def create(
    accountId: Int,
    title: String,
    text: String,
    articleType: ArticleType,
    genre: String,
    optAuthor: Option[String],
    optDate: Option[String],
    editable: Boolean
  ) = db.withSession { implicit session: Session =>
    val date = new Date()
    val id = Articles.ins.insert(accountId, accountId, title, text, date, date, articleType, genre, optAuthor, optDate, editable)
    id
  }

  def update(id: Long, accountId: Int, title: String, text: String, genre: String, optAuthor: Option[String], optDate: Option[String], editable: Boolean) = db.withSession { implicit session: Session =>
    findById(id).map { before =>
      val date = new Date()
      val after = before.copy(updateAccountId = accountId, title = title, text = text, updateDate = date, genre = genre, optAuthor = optAuthor, optDate = optDate, editable = editable)
      query.where(_.id === id).update(after)
    }
  }

  def delete(id: Long) = db.withSession { implicit session: Session =>
    findById(id).map { article =>
      query.where(_.id === id).delete
    }
  }
}

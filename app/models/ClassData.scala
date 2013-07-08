package models

import play.api.db._
import play.api.Play.current

import scala.slick.driver.H2Driver.simple._

import andon.utils._

case class ClassData(id: ClassId, title: String, prize: Option[Prize])

object ClassData extends Table[ClassData]("CLASSDATA") {

  val db = Database.forDataSource(DB.getDataSource("default"))

  def id = column[ClassId]("ID", O.NotNull, O.PrimaryKey)
  def times = column[Int]("TIMES", O.NotNull)
  def grade = column[Int]("GRADE", O.NotNull)
  def classn = column[Int]("CLASSN", O.NotNull)
  def title = column[String]("TITLE", O.NotNull)
  def prize = column[Option[String]]("PRIZE")

  def * = id ~ times ~ grade ~ classn ~ title ~ prize <> (
    (id, times, grade, classn, title, prize) => ClassData(id, title, prize.flatMap(Prize.fromString)),
    data => Some((data.id, data.id.times.n, data.id.grade, data.id.classn, data.title, data.prize.map(_.toString)))
  )

  val query = Query(ClassData)

  def findByClassId(c: ClassId): Option[ClassData] = db.withSession { implicit session: Session =>
    query.filter(_.id === c).firstOption
  }

  def all = db.withSession { implicit session: Session =>
    query.list
  }

  def findByTimes(t: OrdInt) = db.withSession { implicit session: Session =>
    query.filter(_.times === t.n).list
  }

  def search(times: String, prize: String, grade: String, tag: String) = db.withSession { implicit session: Session =>

    val q = if (times == "all") {
      query
    } else {
      query.filter(_.times === times.toInt)
    }

    val q1 = if (prize == "all") {
      q
    } else if (prize == "none") {
      q.filter(_.prize.isNull)
    } else {
      q.filter(_.prize === prize)
    }

    val q2 = if (grade == "all") {
      q1
    } else {
      q1.filter(_.grade === grade.toInt)
    }

    val q3 = if (tag == "all") {
      q2
    } else {
      val tagId = for {
        t <- Tags if t.tag === tag
      } yield t.classId

      for {
        id <- tagId
        data <- q2
        if data.id === id
      } yield data
    }

    q3.list
  }

  def create(data: ClassData) = db.withSession { implicit session: Session =>
    ClassData.insert(data)
  }
}

package controllers

import play.api.mvc._

import andon.utils._
import models._

trait Authentication {

  private def userid(request: RequestHeader) = request.session.get("userid")

  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Artisan.login)

  def IsAuthenticated(f: => Int => Request[AnyContent] => Result) =
    Security.Authenticated(userid, onUnauthorized) { id =>
      Action(request => f(id.toInt)(request))
    }

  def IsValidAccount(f: => Account => Request[AnyContent] => Result) = IsAuthenticated { userid => request =>
    Accounts.findById(userid).map { account =>
      f(account)(request)
    }.getOrElse(Results.Forbidden)
  }

  def HasAuthority(level: AccountLevel)(f: => Account => Request[AnyContent] => Result) = IsValidAccount { account => request =>
    if (AccountLevel.gte(account.level, level)) {
      f(account)(request)
    } else {
      Results.Forbidden
    }
  }

  def CaseAuthority
    (admin: => Account => Request[AnyContent] => Result)
    (master: => Account => Request[AnyContent] => Result)
    (writer: => Account => Request[AnyContent] => Result) =
    IsValidAccount { account => request =>
      account.level match {
        case Admin => admin(account)(request)
        case Master => master(account)(request)
        case Writer => writer(account)(request)
      }
    }

  def IsEditableArticle(id: Long)(f: => Account => Article => Request[AnyContent] => Result) = IsValidAccount { account => request =>
    val article = Articles.findById(id)
    (account.level match {
      case Admin | Master => article.map { a =>
        f(account)(a)(request)
      }
      case Writer => article.map { a =>
        if (a.createAccountId == account.id) {
          f(account)(a)(request)
        } else {
          Results.Forbidden
        }
      }
    }).getOrElse(Results.NotFound(views.html.errors.notFound("/artisan/article?id=" + id.toString)))
  }

  def IsEditableAccount(id: Int)(f: => Account => Account => Request[AnyContent] => Result) = IsValidAccount { me => request =>
    Accounts.findById(id).map { account =>
      val l = account.level
      val mine = id == me.id
      me.level match {
        case Admin if mine || l == Master || l == Writer => f(me)(account)(request)
        case Master if mine || l == Writer => f(me)(account)(request)
        case Writer if mine => f(me)(account)(request)
        case _ => Results.Forbidden
      }
    }.getOrElse(Results.NotFound(views.html.errors.notFound("/artisan/account?id=" + id.toString)))
  }
}

package andon.api

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.control.NonFatal

import akka.stream.ActorFlowMaterializer
import akka.http.server._, Directives._
import akka.http.marshalling.ToResponseMarshallable
import akka.http.marshalling.Marshaller._
import akka.http.unmarshalling.Unmarshaller._

import andon.api.util.Json4sJacksonSupport._
import andon.api.util.Errors
import andon.api.controllers._

object Routes {

  def route(version: String)(implicit ec: ExecutionContext, fm: ActorFlowMaterializer): Route = {
    val exceptionHandler = ExceptionHandler {
      case NonFatal(e) => complete {
        Errors.Unexpected(e)
      }
    }
    handleExceptions(exceptionHandler) {
      pathPrefix(version) {
        articles
      } ~
      complete {
        // catch-all
        Errors.ApiNotFound
      }
    }
  }

  private def articles(implicit ec: ExecutionContext, fm: ActorFlowMaterializer): Route = {
    path("articles") {
      get {
        parameterMap { params =>
          complete {
            ArticleController.all(params)
          }
        }
      } ~
      post {
        entity(as[CreateArticle]) { article =>
          complete {
            ArticleController.add(article)
          }
        } ~
        complete {
          Errors.JsonError
        }
      }
    } ~
    path("articles" / LongNumber) { id =>
      get {
        complete {
          ArticleController.get(id)
        }
      }
    }
  }
}
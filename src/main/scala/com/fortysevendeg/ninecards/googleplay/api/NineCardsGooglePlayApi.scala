package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.Actor
import cats.~>
import cats.free.Free
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import io.circe.generic.auto._
import scalaz.concurrent.Task
import spray.http.StatusCodes.NotFound
import spray.routing.{Directives, HttpService, Route}
import NineCardsMarshallers.TRMFactory

class NineCardsGooglePlayActor extends Actor with HttpService {

  override def actorRefFactory = context

  private val apiRoute: Route = {

    val interpreter: GooglePlay.Ops ~> Task = Wiring.interpreter()
    implicit val trmFactory: TRMFactory[ GooglePlay.FreeOps ] =
      NineCardsMarshallers.contraNaturalTransformFreeTRMFactory[GooglePlay.Ops, Task](
        interpreter, taskMonad, NineCardsMarshallers.TaskMarshallerFactory)
    new NineCardsGooglePlayApi[GooglePlay.Ops]().googlePlayApiRoute
  }

  import AuthHeadersRejectionHandler._

  def receive = runRoute(apiRoute)

}

class NineCardsGooglePlayApi[Ops[_]] (
  implicit
  googlePlayService: GooglePlay.Service[Ops],
  marshallerFactory: TRMFactory[({type L[A] = Free[Ops, A]})#L ]
){
  import CustomDirectives._
  import CustomMatchers._
  import Directives._
  import NineCardsMarshallers._
  import marshallerFactory._

  val googlePlayApiRoute: Route =
    pathPrefix("googleplay") {
      requestHeaders { _auth =>
        cardsRoute ~
        packageRoute ~
        packagesRoute ~
        recommendationsRoute
      }
    }

  private[this] lazy val packageRoute: Route =
    pathPrefix("package") {
      requestHeaders { authParams =>
        path(Segment) { packageName =>
          get {
            complete ( googlePlayService.resolve( authParams, Package(packageName)) )
          }
        }
      }
    }

  private[this] lazy val packagesRoute: Route =
    pathPrefix("packages" / "detailed" ) {
      post {
        requestHeaders { authParams =>
          entity(as[PackageList]){ req =>
            complete ( googlePlayService.resolveMany(authParams, req) )
          }
        }
      }
    }

  private[this] lazy val cardsRoute: Route =
    pathPrefix("cards") {
      requestHeaders { authParams =>
        pathEndOrSingleSlash {
          post {
            entity(as[PackageList]) { packageList =>
              complete ( googlePlayService.getCardList( authParams, packageList) )
            }
          }
        } ~
        pathPrefix(Segment) { packageName =>
          get {
            complete ( googlePlayService.getCard( authParams, Package(packageName)) )
          }
        }
      }
    }

  private[this] lazy val recommendationsRoute: Route =
    pathPrefix("recommendations") {
      requestHeaders { authParams =>
        pathPrefix(CategorySegment) { category =>
          priceFilterPath { filter =>
            get {
              complete ( googlePlayService.recommendationsByCategory(authParams, category, filter) )
            }
          }
        }
      }
    }

}

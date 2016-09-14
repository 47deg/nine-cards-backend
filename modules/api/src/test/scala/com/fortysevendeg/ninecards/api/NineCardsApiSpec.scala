package com.fortysevendeg.ninecards.api

import akka.actor.ActorSystem
import akka.testkit._
import cats.free.Free
import cats.syntax.xor._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.TestData._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes._
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._
import com.fortysevendeg.ninecards.services.common.ConnectionIOOps._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.{ HttpRequest, MediaTypes, StatusCodes }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt
import scalaz.concurrent.Task

trait NineCardsApiSpecification
  extends Specification
  with AuthHeadersRejectionHandler
  with HttpService
  with JsonFormats
  with Matchers
  with Mockito
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  import NineCardsMarshallers._

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  trait BasicScope extends Scope {

    implicit val userProcesses: UserProcesses[NineCardsServices] = mock[UserProcesses[NineCardsServices]]

    implicit val googleApiProcesses: GoogleApiProcesses[NineCardsServices] = mock[GoogleApiProcesses[NineCardsServices]]

    implicit val applicationProcesses: ApplicationProcesses[NineCardsServices] = mock[ApplicationProcesses[NineCardsServices]]

    implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

    implicit val recommendationsProcesses: RecommendationsProcesses[NineCardsServices] = mock[RecommendationsProcesses[NineCardsServices]]

    implicit val sharedCollectionProcesses: SharedCollectionProcesses[NineCardsServices] = mock[SharedCollectionProcesses[NineCardsServices]]

    import com.fortysevendeg.ninecards.services.persistence.CustomComposite._

    val nineCardsApi = new NineCardsRoutes().nineCardsRoutes

    userProcesses.checkAuthToken(
      sessionToken = mockEq(sessionToken),
      androidId    = mockEq(androidId),
      authToken    = mockEq(authToken),
      requestUri   = any[String]
    ) returns Free.pure(Option(userId))
  }

  trait SuccessfulScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns Free.pure(true)

    userProcesses.signUpUser(
      LoginRequest(email, androidId, any, tokenId)
    ) returns Free.pure(Messages.loginResponse)

    userProcesses.updateInstallation(mockEq(Messages.updateInstallationRequest))(any) returns
      Free.pure(Messages.updateInstallationResponse)

    sharedCollectionProcesses.createCollection(any) returns
      Free.pure(Messages.createOrUpdateCollectionResponse)

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any[String], any) returns
      Free.pure(Messages.getCollectionByPublicIdentifierResponse.right)

    sharedCollectionProcesses.subscribe(any[String], any[Long]) returns
      Free.pure(Messages.subscribeResponse.right)

    sharedCollectionProcesses.unsubscribe(any[String], any[Long]) returns
      Free.pure(Messages.unsubscribeResponse.right)

    sharedCollectionProcesses.getLatestCollectionsByCategory(any, any, any, any) returns
      Free.pure(Messages.getCollectionsResponse)

    sharedCollectionProcesses.getPublishedCollections(any[Long], any) returns
      Free.pure(Messages.getCollectionsResponse)

    sharedCollectionProcesses.getSubscriptionsByUser(any) returns
      Free.pure(Messages.getSubscriptionsByUserResponse)

    sharedCollectionProcesses.getTopCollectionsByCategory(any, any, any, any) returns
      Free.pure(Messages.getCollectionsResponse)

    sharedCollectionProcesses.updateCollection(any, any, any) returns
      Free.pure(Messages.createOrUpdateCollectionResponse.right)

    applicationProcesses.getAppsInfo(any, any) returns
      Free.pure(Messages.getAppsInfoResponse)

    rankingProcesses.getRanking(any) returns Free.pure(Messages.rankings.getResponse)

    rankingProcesses.reloadRanking(any, any) returns
      Free.pure(Messages.rankings.reloadResponse.right)

    recommendationsProcesses.getRecommendationsByCategory(any, any, any, any, any) returns
      Free.pure(Messages.getRecommendationsByCategoryResponse)

    recommendationsProcesses.getRecommendationsForApps(any, any, any, any) returns
      Free.pure(Messages.getRecommendationsByCategoryResponse)
  }

  trait UnsuccessfulScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns Free.pure(false)

    userProcesses.checkAuthToken(
      sessionToken = mockEq(sessionToken),
      androidId    = mockEq(androidId),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns Free.pure(None)

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any[String], any) returns
      Free.pure(Exceptions.sharedCollectionNotFoundException.left)

    sharedCollectionProcesses.subscribe(any[String], any[Long]) returns
      Free.pure(Exceptions.sharedCollectionNotFoundException.left)

    sharedCollectionProcesses.unsubscribe(any[String], any[Long]) returns
      Free.pure(Exceptions.sharedCollectionNotFoundException.left)

    sharedCollectionProcesses.updateCollection(any, any, any) returns
      Free.pure(Exceptions.sharedCollectionNotFoundException.left)
  }

  trait FailingScope extends BasicScope {

    val checkAuthTokenTask: Task[Option[Long]] = Task.fail(Exceptions.persistenceException)

    val loginTask: Task[LoginResponse] = Task.fail(Exceptions.persistenceException)

    val updateInstallationTask: Task[UpdateInstallationResponse] = Task.fail(Exceptions.persistenceException)

    val getAppsInfoTask: Task[GetAppsInfoResponse] = Task.fail(Exceptions.http4sException)

    val createCollectionTask: Task[CreateOrUpdateCollectionResponse] = Task.fail(Exceptions.persistenceException)

    val getCollectionByIdTask: Task[XorGetCollectionByPublicId] = Task.now(Exceptions.persistenceException.left)

    val getSubscriptionsByUserTask: Task[GetSubscriptionsByUserResponse] = Task.fail(Exceptions.persistenceException)

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns Free.pure(true)

    userProcesses.checkAuthToken(
      sessionToken = mockEq(sessionToken),
      androidId    = mockEq(androidId),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns checkAuthTokenTask.liftF[NineCardsServices]

    userProcesses.signUpUser(LoginRequest(email, androidId, any, tokenId)) returns
      loginTask.liftF[NineCardsServices]

    userProcesses.updateInstallation(mockEq(Messages.updateInstallationRequest))(any) returns
      updateInstallationTask.liftF[NineCardsServices]

    sharedCollectionProcesses.createCollection(any) returns
      createCollectionTask.liftF[NineCardsServices]

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any[String], any) returns
      getCollectionByIdTask.liftF[NineCardsServices]

    sharedCollectionProcesses.getSubscriptionsByUser(any) returns
      getSubscriptionsByUserTask.liftF[NineCardsServices]

    applicationProcesses.getAppsInfo(any, any) returns
      getAppsInfoTask.liftF[NineCardsServices]
  }

}

class NineCardsApiSpec
  extends NineCardsApiSpecification {

  private[this] def unauthorizedNoHeaders(request: HttpRequest) = {

    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {
      request ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {
      request ~> addHeader(RawHeader(headerAndroidId, androidId)) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in new UnsuccessfulScope {
      request ~> addHeaders(Headers.failingUserInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if a persistence error happens" in new FailingScope {
      request ~> addHeaders(Headers.failingUserInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

  }

  private[this] def notFoundSharedCollection(request: HttpRequest) = {
    "return a 404 Not found status code if the shared collection doesn't exist" in new UnsuccessfulScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.NotFound.intValue
      }
    }
  }

  private[this] def internalServerError(request: HttpRequest) = {
    "return 500 Internal Server Error status code if a persistence error happens" in new FailingScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.InternalServerError.intValue
      }
    }
  }

  private[this] def badRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  private[this] def authenticatedBadRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  private[this] def successOk(request: HttpRequest) = {
    "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.OK.intValue
      }
    }
  }

  "nineCardsApi" should {
    "grant access to Swagger documentation" in new BasicScope {
      Get(Paths.apiDocs) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status should be equalTo StatusCodes.OK.intValue
          mediaType === MediaTypes.`text/html`
          responseAs[String] must contain("Swagger")
        }
    }

    "return a 404 NotFound error for an undefined path " in new BasicScope {
      Get(Paths.invalid) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.NotFound.intValue
      }
    }

  }

  "POST /login" should {

    val request = Post(Paths.login, Messages.apiLoginRequest)

    "return a 401 Unauthorized status code if the given email is empty" in new BasicScope {

      Post(Paths.login, Messages.apiLoginRequest.copy(email = "")) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if the given tokenId is empty" in new BasicScope {

      Post(Paths.login, Messages.apiLoginRequest.copy(tokenId = "")) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if the given email address and tokenId are not valid" in new UnsuccessfulScope {

      Post(Paths.login, Messages.apiLoginRequest) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 200 Ok status code if the given email address and tokenId are valid" in new SuccessfulScope {

      Post(Paths.login, Messages.apiLoginRequest) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
    }

    badRequestEmptyBody(Post(Paths.login))

    internalServerError(request)

  }

  "PUT /installations" should {

    val request = Put(Paths.installations, Messages.apiUpdateInstallationRequest)

    unauthorizedNoHeaders(request)

    authenticatedBadRequestEmptyBody(Put(Paths.installations))

    internalServerError(request)

    successOk(request)
  }

  "POST /collections" should {

    val request = Post(Paths.collections, Messages.apiCreateCollectionRequest)

    unauthorizedNoHeaders(request)

    authenticatedBadRequestEmptyBody(Post(Paths.collections))

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/collectionId" should {

    val request = Get(Paths.collectionById) ~> addHeaders(Headers.googlePlayHeaders)

    "return a 404 Not found status code if the shared collection doesn't exist" in new UnsuccessfulScope {

      Get(Paths.collectionById) ~>
        addHeaders(Headers.userInfoHeaders) ~>
        addHeaders(Headers.googlePlayHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.NotFound.intValue
        }
    }

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "PUT /collections/collectionId" should {

    val request = Put(Paths.collectionById, Messages.apiUpdateCollectionRequest)

    authenticatedBadRequestEmptyBody(Put(Paths.collectionById))

    notFoundSharedCollection(request)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/subscriptions" should {

    val request = Get(Paths.subscriptionsByUser)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "PUT /collections/subscriptions/collectionId" should {

    val request = Put(Paths.subscriptionByCollectionId)

    unauthorizedNoHeaders(request)

    notFoundSharedCollection(request)

    internalServerError(request)

    successOk(request)
  }

  "DELETE /collections/subscriptions/collectionId" should {

    val request = Delete(Paths.subscriptionByCollectionId)

    unauthorizedNoHeaders(request)

    notFoundSharedCollection(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections" should {

    val request = Get(Paths.collections) ~>
      addHeaders(Headers.googlePlayHeaders)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/latest/category" should {

    val request = Get(Paths.latestCollections) ~>
      addHeaders(Headers.googlePlayHeaders)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/top/category" should {

    val request = Get(Paths.topCollections) ~>
      addHeaders(Headers.googlePlayHeaders)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "POST /applications/categorize" should {

    val request = Post(
      uri     = Paths.categorize,
      content = Messages.apiGetAppsInfoRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.categorize))

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "POST /applications/details" should {

    val request = Post(
      uri     = Paths.details,
      content = Messages.apiGetAppsInfoRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.details))

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "POST /recommendations" should {

    val request = Post(
      uri     = Paths.recommendationsForApps,
      content = Messages.apiGetRecommendationsForAppsRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.recommendationsByCategory))

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "POST /recommendations/category" should {

    val request = Post(
      uri     = Paths.recommendationsByCategory,
      content = Messages.apiGetRecommendationsByCategoryRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.recommendationsByCategory))

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  def testRanking(scopePath: String) = {
    val path = s"/rankings/$scopePath"

    s""" "GET ${path}", the endpoint to read a ranking,""" should {
      val request = Get(path)

      internalServerError(request)

      "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
        request ~> sealRoute(nineCardsApi) ~> check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
      }

    }

    s""" "POST $path", the endpoint to refresh an ranking,""" should {

      import NineCardsMarshallers._

      val request = Post(path, Messages.rankings.reloadApiRequest)

      "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
        request ~> addHeaders(Headers.googleAnalyticsHeaders) ~> sealRoute(nineCardsApi) ~> check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
      }
    }

  }

  val rankingPaths: List[String] = {
    import com.fortysevendeg.ninecards.services.free.domain.rankings.{ Continent, Country }
    val countries = Country.values.map(c ⇒ s"countries/$c").toList
    val continents = Continent.values.map(c ⇒ s"continents/$c").toList
    "world" :: (continents ++ countries)
  }

  rankingPaths foreach testRanking
}
package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.commons.TaskInstances._
import cards.nine.googleplay.domain.{ Details ⇒ _, _ }
import cards.nine.googleplay.domain.apigoogle._
import cards.nine.googleplay.proto.GooglePlay.{ BulkDetailsRequest, DocV2, ResponseWrapper }
import cards.nine.googleplay.service.free.algebra.GoogleApi._
import cats.~>
import cats.data.Xor
import cats.instances.list._
import cats.syntax.monadCombine._
import cats.syntax.traverse._
import org.http4s.Http4s._
import org.http4s._
import org.http4s.client.{ Client, UnexpectedStatus }

import scala.collection.JavaConversions._
import collection.JavaConverters._
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Interpreter(config: Configuration) extends (Ops ~> WithHttpClient) {

  def apply[A](ops: Ops[A]): WithHttpClient[A] = ops match {
    case GetBulkDetails(packs, auth) ⇒ new BulkDetailsWithClient(packs, auth)
    case GetDetails(pack, auth) ⇒ new DetailsWithClient(pack, auth)
    case RecommendationsByApps(request, auth) ⇒ new RecommendationsByAppsWithClient(request, auth)
    case RecommendationsByCategory(request, auth) ⇒ new RecommendationsByCategoryWithClient(request, auth)
  }

  private[this] val baseUri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)))
  )

  class BulkDetailsWithClient(packagesName: List[Package], auth: GoogleAuthParams)
    extends (Client ⇒ Task[Failure Xor List[FullCard]]) {

    val builder = BulkDetailsRequest.newBuilder()
    builder.addAllDocid(packagesName.map(_.value).asJava)

    val httpRequest =
      new Request(
        method  = Method.POST,
        uri     = baseUri.withPath(config.bulkDetailsPath),
        headers = headers.fullHeaders(auth, Option("application/x-protobuf"))
      ).withBody(builder.build().toByteArray)

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.Unauthorized ⇒ WrongAuthParams(auth)
      case Status.TooManyRequests ⇒ QuotaExceeded(auth)
      case _ ⇒ GoogleApiServerError
    }

    def apply(client: Client): Task[Failure Xor List[FullCard]] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        Xor.right {
          ResponseWrapper
            .parseFrom(bv.toArray)
            .getPayload.getBulkDetailsResponse
            .getEntryList
            .toList
            .map(entry ⇒ Converters.toFullCard(entry.getDoc))
            .filterNot(_.packageName.isEmpty)
        }
      }.handle {
        case e: UnexpectedStatus ⇒ Xor.Left(handleUnexpected(e))
      }
  }

  class DetailsWithClient(packageName: Package, auth: GoogleAuthParams)
    extends (Client ⇒ Task[Failure Xor FullCard]) {

    val httpRequest: Request =
      new Request(
        method  = Method.GET,
        uri     = baseUri
          .withPath(config.detailsPath)
          .withQueryParam("doc", packageName.value),
        headers = headers.fullHeaders(auth)
      )

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.NotFound ⇒ PackageNotFound(packageName)
      case Status.Unauthorized ⇒ WrongAuthParams(auth)
      case Status.TooManyRequests ⇒ QuotaExceeded(auth)
      case _ ⇒ GoogleApiServerError
    }

    def apply(client: Client): Task[Failure Xor FullCard] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        val docV2: DocV2 = ResponseWrapper.parseFrom(bv.toArray).getPayload.getDetailsResponse.getDocV2
        val fullCard = Converters.toFullCard(docV2)
        Xor.Right(fullCard)
      }.handle {
        case e: UnexpectedStatus ⇒ Xor.Left(handleUnexpected(e))
      }
  }

  class RecommendationsByAppsWithClient(request: RecommendByAppsRequest, auth: GoogleAuthParams)
    extends (Client ⇒ Task[List[Package]]) {

    def recommendationsByApp(client: Client)(pack: Package): Task[InfoError Xor List[Package]] = {

      val httpRequest: Request = new Request(
        method  = Method.GET,
        uri     = baseUri
          .withPath(config.recommendationsPath)
          .withQueryParam("c", 3)
          .withQueryParam("rt", "1")
          .withQueryParam("doc", pack.value),
        headers = headers.fullHeaders(auth)
      )

      def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
        case Status.NotFound ⇒ PackageNotFound(pack)
        case Status.Unauthorized ⇒ WrongAuthParams(auth)
        case Status.TooManyRequests ⇒ QuotaExceeded(auth)
        case _ ⇒ GoogleApiServerError
      }

      client.expect[ByteVector](httpRequest).map { bv ⇒
        Xor.Right(ResponseWrapper.parseFrom(bv.toArray).getPayload.getListResponse)
      }.handle {
        case e: UnexpectedStatus ⇒ Xor.Left(handleUnexpected(e))
      }.map {
        _.bimap(
          _ ⇒ InfoError(s"Recommendations for package ${pack.value}"),
          list ⇒ Converters.listResponseToPackages(list).take(request.numPerApp)
        )
      }
    }

    def joinLists(xors: List[InfoError Xor List[Package]]): List[Package] = {
      val (_, packages) = xors.separate
      packages.flatten.distinct.diff(request.excludedApps).take(request.maxTotal)
    }

    def apply(client: Client): Task[List[Package]] = {
      request.searchByApps.traverse(recommendationsByApp(client)).map(joinLists)
    }
  }

  class RecommendationsByCategoryWithClient(request: RecommendByCategoryRequest, auth: GoogleAuthParams)
    extends (Client ⇒ Task[InfoError Xor List[Package]]) {

    val infoError = InfoError(s"Recommendations for category ${request.category} that are ${request.priceFilter}")

    val subCategory: String = request.priceFilter match {
      case PriceFilter.FREE ⇒ "apps_topselling_free"
      case PriceFilter.PAID ⇒ "apps_topselling_paid"
      case PriceFilter.ALL ⇒ "apps_topgrossing"
    }

    val httpRequest: Request = new Request(
      method  = Method.GET,
      uri     = baseUri
        .withPath(config.listPath)
        .withQueryParam("c", "3")
        .withQueryParam("cat", request.category.entryName)
        .withQueryParam("ctr", subCategory),
      headers = headers.fullHeaders(auth)
    )

    def apply(client: Client): Task[InfoError Xor List[Package]] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        Xor.Right(ResponseWrapper.parseFrom(bv.toArray).getPayload.getListResponse)
      }.handle {
        case e: UnexpectedStatus ⇒ Xor.Left(GoogleApiServerError)
      }.map {
        case Xor.Left(_) ⇒ Xor.Left(infoError)
        case Xor.Right(listResponse) ⇒ Xor.right(Converters.listResponseToPackages(listResponse))
      }
  }
}
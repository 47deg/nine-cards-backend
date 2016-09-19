package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.GooglePlayAuthMessages._
import com.fortysevendeg.ninecards.processes.messages.RecommendationsMessages._
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay.Recommendations

class RecommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getRecommendationsByCategory(
    category: String,
    filter: String,
    excludePackages: List[String],
    limit: Int,
    authParams: AuthParams
  ): Free[F, GetRecommendationsResponse] =
    services.recommendByCategory(
      category    = category,
      priceFilter = filter,
      auth        = toAuthParamsServices(authParams)
    ) map generateRecommendationsResponse(excludePackages, limit)

  def getRecommendationsForApps(
    packagesName: List[String],
    excludePackages: List[String],
    limit: Int,
    authParams: AuthParams
  ): Free[F, GetRecommendationsResponse] =
    if (packagesName.isEmpty)
      Free.pure(GetRecommendationsResponse(Nil))
    else
      services.recommendationsForApps(
        packagesName = packagesName,
        auth         = toAuthParamsServices(authParams)
      ) map generateRecommendationsResponse(excludePackages, limit)

  private def generateRecommendationsResponse(excludePackages: List[String], limit: Int)(rec: Recommendations) =
    GetRecommendationsResponse(
      rec.apps
        .filterNot(r ⇒ excludePackages.contains(r.packageName))
        .take(limit)
        .map(toGooglePlayRecommendation)
    )
}

object RecommendationsProcesses {

  implicit def recommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new RecommendationsProcesses

}

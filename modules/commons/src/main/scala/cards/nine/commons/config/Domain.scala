package cards.nine.commons.config

import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import scala.concurrent.duration.{ Duration, FiniteDuration }

object Domain {

  case class NineCardsConfiguration(
    db: DatabaseConfiguration,
    debugMode: Option[Boolean],
    google: GoogleConfiguration,
    http: HttpConfiguration,
    rankings: RankingsConfiguration,
    redis: RedisConfiguration,
    secretKey: String,
    salt: Option[String],
    test: TestConfiguration
  )

  object NineCardsConfiguration {
    def apply(config: NineCardsConfig): NineCardsConfiguration = {
      val prefix = "ninecards"

      NineCardsConfiguration(
        DatabaseConfiguration(config, prefix),
        config.getOptionalBoolean(s"$prefix.debugMode"),
        GoogleConfiguration(config, prefix),
        HttpConfiguration(config, prefix),
        RankingsConfiguration(config, prefix),
        RedisConfiguration(config, prefix),
        config.getString(s"$prefix.secretKey"),
        config.getOptionalString(s"$prefix.salt"),
        TestConfiguration(config, prefix)
      )
    }
  }

  case class DatabaseConfiguration(
    default: DatabaseDefaultConfiguration,
    hikari: DatabaseHikariConfiguration
  )

  object DatabaseConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): DatabaseConfiguration = {
      val prefix = s"$parentPrefix.db"

      DatabaseConfiguration(
        DatabaseDefaultConfiguration(config, prefix),
        DatabaseHikariConfiguration(config, prefix)
      )
    }
  }

  case class DatabaseDefaultConfiguration(
    driver: String,
    url: String,
    user: String,
    password: String
  )

  object DatabaseDefaultConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): DatabaseDefaultConfiguration = {
      val prefix = s"$parentPrefix.default"

      DatabaseDefaultConfiguration(
        config.getString(s"$prefix.driver"),
        config.getString(s"$prefix.url"),
        config.getString(s"$prefix.user"),
        config.getString(s"$prefix.password")
      )
    }
  }

  case class DatabaseHikariConfiguration(
    maximumPoolSize: Int,
    maxLifetime: Int
  )

  object DatabaseHikariConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): DatabaseHikariConfiguration = {
      val prefix = s"$parentPrefix.hikari"

      DatabaseHikariConfiguration(
        config.getInt(s"$prefix.maximumPoolSize"),
        config.getInt(s"$prefix.maxLifetime")
      )
    }
  }

  case class GoogleConfiguration(
    analytics: GoogleAnalyticsConfiguration,
    api: GoogleApiConfiguration,
    firebase: GoogleFirebaseConfiguration,
    play: GooglePlayConfiguration
  )

  object GoogleConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleConfiguration = {
      val prefix = s"$parentPrefix.google"

      GoogleConfiguration(
        GoogleAnalyticsConfiguration(config, prefix),
        GoogleApiConfiguration(config, prefix),
        GoogleFirebaseConfiguration(config, prefix),
        GooglePlayConfiguration(config, prefix)
      )
    }
  }

  case class GoogleAnalyticsConfiguration(
    protocol: String,
    host: String,
    port: Option[Int],
    path: String,
    viewId: String
  )

  object GoogleAnalyticsConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleAnalyticsConfiguration = {
      val prefix = s"$parentPrefix.analytics"

      GoogleAnalyticsConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getOptionalInt(s"$prefix.port"),
        config.getString(s"$prefix.path"),
        config.getString(s"$prefix.viewId")
      )
    }
  }

  case class GoogleApiConfiguration(
    protocol: String,
    host: String,
    port: Option[Int],
    tokenInfo: GoogleApiTokenInfo
  )

  object GoogleApiConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleApiConfiguration = {
      val prefix = s"$parentPrefix.api"

      GoogleApiConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getOptionalInt(s"$prefix.port"),
        GoogleApiTokenInfo(config, prefix)
      )
    }
  }

  case class GoogleApiTokenInfo(
    path: String,
    tokenIdQueryParameter: String
  )

  object GoogleApiTokenInfo {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleApiTokenInfo = {
      val prefix = s"$parentPrefix.tokenInfo"

      GoogleApiTokenInfo(
        config.getString(s"$prefix.path"),
        config.getString(s"$prefix.tokenIdQueryParameter")
      )
    }
  }

  case class GoogleFirebaseConfiguration(
    protocol: String,
    host: String,
    port: Option[Int],
    authorizationKey: String,
    paths: GoogleFirebasePaths
  )

  object GoogleFirebaseConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleFirebaseConfiguration = {
      val prefix = s"$parentPrefix.firebase"

      GoogleFirebaseConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getOptionalInt(s"$prefix.port"),
        config.getString(s"$prefix.authorizationKey"),
        GoogleFirebasePaths(config, prefix)
      )
    }
  }

  case class GoogleFirebasePaths(sendNotification: String)

  object GoogleFirebasePaths {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleFirebasePaths = {
      val prefix = s"$parentPrefix.paths"

      GoogleFirebasePaths(
        config.getString(s"$prefix.sendNotification")
      )
    }
  }

  case class GooglePlayConfiguration(
    api: GooglePlayApiConfiguration,
    web: GooglePlayWebConfiguration
  )

  object GooglePlayConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayConfiguration = {
      val prefix = s"$parentPrefix.play"

      GooglePlayConfiguration(
        GooglePlayApiConfiguration(config, prefix),
        GooglePlayWebConfiguration(config, prefix)
      )
    }
  }

  case class GooglePlayApiConfiguration(
    protocol: String,
    host: String,
    port: Int,
    paths: GooglePlayApiPaths
  )

  object GooglePlayApiConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayApiConfiguration = {
      val prefix = s"$parentPrefix.api"

      GooglePlayApiConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port"),
        GooglePlayApiPaths(config, prefix)
      )
    }
  }

  case class GooglePlayApiPaths(
    bulkDetails: String,
    details: String,
    list: String,
    search: String,
    recommendations: String
  )

  object GooglePlayApiPaths {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayApiPaths = {
      val prefix = s"$parentPrefix.paths"

      GooglePlayApiPaths(
        config.getString(s"$prefix.bulkDetails"),
        config.getString(s"$prefix.details"),
        config.getString(s"$prefix.list"),
        config.getString(s"$prefix.search"),
        config.getString(s"$prefix.recommendations")
      )
    }
  }

  case class GooglePlayWebConfiguration(
    protocol: String,
    host: String,
    port: Int,
    paths: GooglePlayWebPaths
  )

  object GooglePlayWebConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayWebConfiguration = {
      val prefix = s"$parentPrefix.web"

      GooglePlayWebConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port"),
        GooglePlayWebPaths(config, prefix)
      )
    }
  }

  case class GooglePlayWebPaths(details: String)

  object GooglePlayWebPaths {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayWebPaths = {
      val prefix = s"$parentPrefix.paths"

      GooglePlayWebPaths(
        config.getString(s"$prefix.details")
      )
    }
  }

  case class HttpConfiguration(
    host: String,
    port: Int
  )

  object HttpConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): HttpConfiguration = {
      val prefix = s"$parentPrefix.http"

      HttpConfiguration(
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port")
      )
    }
  }

  case class RankingsConfiguration(
    actorInterval: FiniteDuration,
    rankingPeriod: Period,
    countriesPerRequest: Int,
    maxNumberOfAppsPerCategory: Int
  )

  object RankingsConfiguration {
    def convertToFiniteDuration(value: String) = {
      val duration = Duration(value)
      FiniteDuration(duration._1, duration._2)
    }

    def apply(config: NineCardsConfig, parentPrefix: String): RankingsConfiguration = {
      val prefix = s"$parentPrefix.rankings"

      RankingsConfiguration(
        convertToFiniteDuration(config.getString(s"$prefix.actorInterval")),
        PeriodFormat.getDefault.parsePeriod(config.getString(s"$prefix.rankingPeriod")),
        config.getInt(s"$prefix.countriesPerRequest"),
        config.getInt(s"$prefix.maxNumberOfAppsPerCategory")
      )
    }
  }

  case class RedisConfiguration(
    host: String,
    port: Int,
    secret: Option[String]
  )

  object RedisConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): RedisConfiguration = {
      val prefix = s"$parentPrefix.redis"

      RedisConfiguration(
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port"),
        config.getOptionalString(s"$prefix.secret")
      )
    }
  }

  case class TestConfiguration(
    androidId: String,
    token: String,
    localization: String,
    googlePlayDetailsUrl: String
  )

  object TestConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): TestConfiguration = {
      val prefix = s"$parentPrefix.test"

      TestConfiguration(
        config.getString(s"$prefix.androidId"),
        config.getString(s"$prefix.token"),
        config.getString(s"$prefix.localization"),
        config.getString(s"$prefix.googlePlayDetailsAppUrl")

      )
    }
  }

}
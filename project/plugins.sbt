resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "Flyway" at "http://flywaydb.org/repo"

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.2.1")

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "3.2.1")

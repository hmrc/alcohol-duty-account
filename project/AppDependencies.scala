import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.3.0"
  private val hmrcMongoVersion = "2.10.0"
  val mockitoScalaVersion      = "2.0.0"

  val compile = Seq(
    "org.typelevel"     %% "cats-core"                 % "2.13.0",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % hmrcMongoVersion,
    "com.beachape"      %% "enumeratum-play-json"      % "1.9.0"
  )

  val test           = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"   % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"  % hmrcMongoVersion    % Test,
    "org.mockito"       %% "mockito-scala"            % mockitoScalaVersion % Test,
    "org.mockito"       %% "mockito-scala-cats"       % mockitoScalaVersion % Test,
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2"       % Test
  )
  val itDependencies = Seq.empty
}

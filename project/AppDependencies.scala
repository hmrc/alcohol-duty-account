import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.11.0"
  val mockitoScalaVersion = "1.17.37"

  val compile = Seq(
    "org.typelevel"           %% "cats-core"                  % "2.12.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.beachape"            %% "enumeratum-play-json"       % "1.8.1"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "org.mockito"             %% "mockito-scala"              % mockitoScalaVersion         % Test,
    "org.mockito"             %% "mockito-scala-cats"         % mockitoScalaVersion         % Test,
    "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"               % Test
  )
  val itDependencies = Seq.empty
}

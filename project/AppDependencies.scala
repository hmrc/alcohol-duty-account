import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.5.0"
  val mockitoScalaVersion = "3.2.17.0"

  val compile = Seq(
    "org.typelevel"           %% "cats-core"                  % "2.12.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.beachape"            %% "enumeratum-play-json"       % "1.8.1"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "org.scalatestplus"             %% "mockito-4-11"              % mockitoScalaVersion                   % Test,
    // TODO "org.scalatestplus"             %% "mockito-scala-cats"         % mockitoScalaVersion                   % Test,
    "org.scalatestplus"       %% "scalacheck-1-18"   % "3.2.19.0"               % Test
  )
  val itDependencies = Seq.empty
}

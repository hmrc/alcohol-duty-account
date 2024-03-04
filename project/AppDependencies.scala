import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"

  val compile = Seq(
    "org.typelevel"           %% "cats-core"                  % "2.10.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "org.mockito"             %% "mockito-scala"              % "1.17.30"                   % Test,
    "org.mockito"             %% "mockito-scala-cats"         % "1.17.30"                   % Test
  )
  val itDependencies = Seq.empty
}

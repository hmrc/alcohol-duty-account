/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.alcoholdutyaccount.base

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{Status => _, _}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test._
import play.api.{Application, Mode}
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

abstract class ISpecBase
    extends AnyFreeSpec
    with GuiceOneServerPerSuite
    with Matchers
    with Inspectors
    with ScalaFutures
    with DefaultAwaitTimeout
    with Writeables
    with EssentialActionCaller
    with RouteInvokers
    with LoneElement
    with Inside
    with OptionValues
    with Results
    with Status
    with HeaderNames
    with MimeTypes
    with HttpProtocol
    with HttpVerbs
    with ResultExtractors
    with WireMockSupport
    with AuthStubs
    with IntegrationPatience
    with TestData {

  implicit lazy val arbString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr.retryUntil(_.nonEmpty))

  implicit lazy val system: ActorSystem        = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit def ec: ExecutionContext            = global

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"  -> false,
    "auditing.enabled" -> false
  ) ++ getWireMockAppConfig(Seq("auth", "subscription", "obligation", "financial"))

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[com.codahale.metrics.MetricRegistry]
      .configure(additionalAppConfig)
      .in(Mode.Test)
      .build()

  lazy val config = new AppConfig(app.configuration, new ServicesConfig(app.configuration))

  /*
  This is to initialise the app before running any tests, as it is lazy by default in org.scalatestplus.play.BaseOneAppPerSuite.
  It enables us to include behaviour tests that call routes within the `must` part of a test but before `in`.
   */
  locally { val _ = app }

  def callRoute[A](fakeRequest: FakeRequest[A], requiresAuth: Boolean = true)(implicit
    app: Application,
    w: Writeable[A]
  ): Future[Result] = {
    val errorHandler = app.errorHandler

    val req = if (requiresAuth) fakeRequest.withHeaders("Authorization" -> "test") else fakeRequest

    route(app, req) match {
      case None         => fail("Route does not exist")
      case Some(result) =>
        result.recoverWith { case t: Throwable =>
          errorHandler.onServerError(req, t)
        }
    }
  }
}

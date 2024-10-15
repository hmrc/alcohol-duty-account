/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, PlayBodyParsers, Results}
import play.api.test.Helpers.stubControllerComponents
import play.api.test.{DefaultAwaitTimeout, FakeHeaders, FakeRequest, ResultExtractors}
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.common.generators.ModelGenerators
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.connectors.helpers.RandomUUIDGenerator
import uk.gov.hmrc.alcoholdutyaccount.controllers.actions.{FakeAuthorisedAction, FakeCheckAppaIdAction}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with Results
    with DefaultAwaitTimeout
    with ResultExtractors
    with Status
    with HeaderNames
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach
    with TestData
    with ModelGenerators
    with IntegrationPatience {

  def configOverrides: Map[String, Any] = Map()

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"  -> false,
    "auditing.enabled" -> false
  ) ++ configOverrides

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .build()

  val cc: ControllerComponents                         = stubControllerComponents()
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val appConfig: AppConfig                             = app.injector.instanceOf[AppConfig]
  val bodyParsers: PlayBodyParsers                     = app.injector.instanceOf[PlayBodyParsers]
  val fakeAuthorisedAction                             = new FakeAuthorisedAction(bodyParsers)
  val fakeCheckAppaIdAction                            = new FakeCheckAppaIdAction()

  def fakeRequestWithJsonBody(json: JsValue): FakeRequest[JsValue] = FakeRequest("", "/", FakeHeaders(), json)

  val fakeUUIDGenerator = new RandomUUIDGenerator {
    override def uuid: String = dummyUUID
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()
}

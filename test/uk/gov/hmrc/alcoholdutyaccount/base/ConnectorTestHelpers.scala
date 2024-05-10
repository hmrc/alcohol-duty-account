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

import org.scalatest.Suite
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.alcoholdutyaccount.common.WireMockHelper
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientSupport, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait ConnectorTestHelpers extends WireMockSupport with HttpClientSupport with WireMockHelper {
  this: Suite =>

  protected val endpointName: String

  protected abstract class ConnectorFixture {
    private val application =
      new GuiceApplicationBuilder()
        .configure(getWireMockAppConfig(Seq(endpointName)))
        .build()

    val config = new AppConfig(application.configuration, new ServicesConfig(application.configuration))

    protected implicit val hc: HeaderCarrier = HeaderCarrier()
  }
}

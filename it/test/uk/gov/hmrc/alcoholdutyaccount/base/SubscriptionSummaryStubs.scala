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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.WireMockHelper.stub
import uk.gov.hmrc.alcoholdutyaccount.models.hods.SubscriptionSummary

trait SubscriptionSummaryStubs { self: WireMockStubs =>

  def stubGetSubscriptionSummary(subscriptionSummary: SubscriptionSummary ): StubMapping =
    stub(
      get(
        urlEqualTo(
          s"/subscription/AD/ZAD/$alcoholDutyReference/summary"
        )
      ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(subscriptionSummary).toString())
    )

  def stubSubscriptionSummaryNotFound(): StubMapping =
    stub(
      get(
        urlEqualTo(
          s"/subscription/AD/ZAD/$alcoholDutyReference/summary"
        )
      ),
      aResponse()
        .withStatus(NOT_FOUND)
        .withBody("No obligation data found")
    )
}

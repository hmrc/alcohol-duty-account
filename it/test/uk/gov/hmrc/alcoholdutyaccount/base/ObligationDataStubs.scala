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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.WireMockHelper.stub
import uk.gov.hmrc.alcoholdutyaccount.models.hods.ObligationData

trait ObligationDataStubs { self: WireMockStubs =>

  def stubGetObligations(obligationData: ObligationData): StubMapping =
    stub(
      get(
        urlEqualTo(
          s"/enterprise/obligation-data/ZAD/$alcoholDutyReference/AD?status=O"
        )
      ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(obligationData).toString())
    )

  def stubObligationsNotFound(): StubMapping =
    stub(
      get(
        urlEqualTo(
          s"/enterprise/obligation-data/ZAD/$alcoholDutyReference/AD?status=O"
        )
      ),
      aResponse()
        .withStatus(NOT_FOUND)
        .withBody("No obligation data found")
    )

  def stubObligationsError(): StubMapping =
    stub(
      get(
        urlEqualTo(
          s"/enterprise/obligation-data/ZAD/$alcoholDutyReference/AD?status=O"
        )
      ),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("No obligation data found")
    )
}
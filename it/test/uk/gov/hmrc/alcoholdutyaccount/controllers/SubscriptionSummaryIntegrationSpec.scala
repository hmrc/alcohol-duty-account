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

package uk.gov.hmrc.alcoholdutyaccount.controllers

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, ISpecBase}
import uk.gov.hmrc.alcoholdutyaccount.common.AlcoholDutyTestData
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class SubscriptionSummaryIntegrationSpec extends ISpecBase with ConnectorTestHelpers {
  protected val endpointName = "subscription"

  "the subscription summary endpoint should" should {
    "respond with OK if able to fetch subscription summary data" in new SetUp {
      stubAuthorised()
      stubGet(url, OK, Json.toJson(approvedSubscriptionSummary).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.subscriptionSummary(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(approvedSubscriptionSummary)

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised()
      stubGet(url, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.subscriptionSummary(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary"))

      verifyGet(url)
    }

    "respond with NOT_FOUND if subscription summary found" in new SetUp {
      stubAuthorised()
      stubGet(url, NOT_FOUND, "")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.subscriptionSummary(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Subscription summary not found"))

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the subscription summary api call" in new SetUp {
      stubAuthorised()
      stubGet(url, BAD_REQUEST, "")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.subscriptionSummary(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))

      verifyGet(url)
    }
  }

  class SetUp extends AlcoholDutyTestData {
    val url       =
      s"${config.subscriptionApiUrl}/subscription/${config.regimeType}/${config.idType}/$alcoholDutyReference/summary"
  }
}

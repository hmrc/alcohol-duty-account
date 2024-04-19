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
import uk.gov.hmrc.alcoholdutyaccount.base.ISpecBase
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class ObligationDetailsIntegrationSpec
  extends ISpecBase {

  "the obligation details endpoint should" should {
    "respond with 200 if able to fetch data that matches the period key" in {
      stubAuthorised()
      stubGetObligations(obligationDataSingleOpen)

      val periodKey = "24AE"

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(alcoholDutyReference, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(obligationDetails)
    }

    "respond with NOT_FOUND if the period key doesn't match any open obligation details" in {
      stubAuthorised()
      stubGetObligations(obligationDataSingleOpen)

      val periodKey = "24AF"

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(alcoholDutyReference, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
    }

    "respond with 500 if errors in the obligation api call" in {
      stubAuthorised()
      stubObligationsError()

      val periodKey = "24AE"

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(alcoholDutyReference, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
    }
  }
}

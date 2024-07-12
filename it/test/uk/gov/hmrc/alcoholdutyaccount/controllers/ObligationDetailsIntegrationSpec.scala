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
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.models.hods.Open
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class ObligationDetailsIntegrationSpec extends ISpecBase {
  protected val endpointName = "obligation"

  "the open obligation details endpoint should" should {
    "respond with OK if able to fetch data that matches the period key" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, expectedQueryParams, OK, Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(adrObligationDetails)

      verifyGetWithParameters(url, expectedQueryParams)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, expectedQueryParams, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))

      verifyGetWithParameters(url, expectedQueryParams)
    }

    "respond with NOT_FOUND if no obligation data" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, expectedQueryParams, NOT_FOUND, notFoundErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Obligation data not found"))

      verifyGetWithParameters(url, expectedQueryParams)
    }

    "respond with NOT_FOUND if the period key doesn't match any open obligation details" in new SetUp {
      val testPeriodKey = periodKey4

      stubAuthorised()
      stubGetWithParameters(url, expectedQueryParams, OK, Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, testPeriodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, s"Obligation details not found for period key $testPeriodKey"))

      verifyGetWithParameters(url, expectedQueryParams)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the obligation api call" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, expectedQueryParams, BAD_REQUEST, otherErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))

      verifyGetWithParameters(url, expectedQueryParams)
    }
  }

  "the obligation details endpoint should" should {
    "respond with OK if able to fetch data" in new SetUp {
      stubAuthorised()

      stubGet(url,OK,Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(Seq(adrObligationDetails))
      verifyGet(url)
    }
    "respond with OK if able to fetch open and fulfilled obligation data" in new SetUp {
      stubAuthorised()
      stubGet(url, OK, Json.toJson(obligationDataMultipleOpenAndFulfilled).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(adrMultipleOpenAndFulfilledData)

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised()
      stubGet(url, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))

      verifyGet(url)
    }

    "respond with NOT_FOUND if no obligation data" in new SetUp {
      stubAuthorised()
      stubGet(url, NOT_FOUND, notFoundErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Obligation data not found"))

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the obligation api call" in new SetUp {
      stubAuthorised()
      stubGet(url, BAD_REQUEST, otherErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))

      verifyGet(url)
    }
  }

  class SetUp extends TestData {

    val expectedQueryParams = Seq("status" -> Open.value)

    val url                 =
      s"${config.obligationDataHost}/enterprise/obligation-data/${config.idType}/$appaId/${config.regime}"

    val notFoundErrorMessage = """{
                                 |    "code": "NOT_FOUND",
                                 |    "reason": "The remote endpoint has indicated that no associated data found."
                                 |}
                                 |""".stripMargin

    val otherErrorMessage =
      """
        |{
        |    "failures": [
        |        {
        |            "code": "INVALID_IDTYPE",
        |            "reason": "Submission has not passed validation. Invalid parameter idType"
        |        },
        |        {
        |            "code": "INVALID_IDNUMBER",
        |            "reason": "Submission has not passed validation. Invalid parameter idNumber"
        |        }
        |    ]
        |}
        |""".stripMargin
  }
}

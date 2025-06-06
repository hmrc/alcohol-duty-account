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

import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.alcoholdutyaccount.base.ISpecBase
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.models.AdrObligationData
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Fulfilled, Open}
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.{Clock, LocalDate}

class ObligationDetailsIntegrationSpec extends ISpecBase {
  protected val endpointName = "obligation"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .overrides(bind(classOf[Clock]).toInstance(clock))
      .build()

  "the open obligation details endpoint must" - {
    "respond with OK if able to fetch data that matches the period key" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(adrObligationDetails)

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(
        ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data")
      )

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with an empty obligations document if no open obligation data" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, NOT_FOUND, notFoundErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe NOT_FOUND
      contentAsJson(response) mustBe Json.toJson(
        ErrorResponse(NOT_FOUND, s"Obligation details not found for period key $periodKey")
      )

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with NOT_FOUND if the period key doesn't match any open obligation details" in new SetUp {
      val testPeriodKey = periodKey4

      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, testPeriodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe NOT_FOUND
      contentAsJson(response) mustBe Json.toJson(
        ErrorResponse(NOT_FOUND, s"Obligation details not found for period key $testPeriodKey")
      )

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with BAD_REQUEST if error(s) returned from the obligation api call" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, BAD_REQUEST, otherErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.openObligationDetails(appaId, periodKey).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe BAD_REQUEST
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(BAD_REQUEST, "Bad request"))

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }
  }

  "the obligation details endpoint must" - {
    "respond with OK if able to fetch data" in new SetUp {
      stubAuthorised(appaId)

      stubGetWithParameters(url, expectedQueryParamsNoStatus, OK, Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(Seq(adrObligationDetails))
      verifyGetWithParametersAndHeaders(url, expectedQueryParamsNoStatus, expectedHeaders)
    }

    "respond with OK if able to fetch open and fulfilled obligation data" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(
        url,
        expectedQueryParamsNoStatus,
        OK,
        Json.toJson(obligationDataMultipleOpenAndFulfilled).toString
      )

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(adrMultipleOpenAndFulfilledData)

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsNoStatus, expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsNoStatus, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(
        ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data")
      )

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsNoStatus, expectedHeaders)
    }

    "respond with no obligations if no obligation data" in new SetUp {
      stubAuthorised(appaId)

      stubGetWithParameters(url, expectedQueryParamsNoStatus, NOT_FOUND, notFoundErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(Seq.empty[AdrObligationData])

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsNoStatus, expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the obligation api call" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsNoStatus, BAD_GATEWAY, otherErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.obligationDetails(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsNoStatus, expectedHeaders)
    }
  }

  class SetUp extends TestData {

    val expectedQueryParamsOpen = Seq("status" -> Open.value)

    val expectedHeaders = Seq(
      HeaderNames.authorisation -> s"Bearer ${config.obligationDataToken}",
      "Environment"             -> config.obligationDataEnv
    )

    private val dateFilterHeadersHeaders =
      Seq("from" -> "2023-09-01", "to" -> LocalDate.now(clock).toString)

    val expectedQueryParamsFulfilled     =
      Seq("status" -> Fulfilled.value) ++ dateFilterHeadersHeaders

    val expectedQueryParamsNoStatus      = dateFilterHeadersHeaders

    val url = config.obligationDataUrl(appaId)

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

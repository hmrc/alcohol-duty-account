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
import play.api.test.Helpers.await
import uk.gov.hmrc.alcoholdutyaccount.base.ISpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.hods._
import uk.gov.hmrc.alcoholdutyaccount.models.{AdrObligationData, FulfilledObligations}
import uk.gov.hmrc.alcoholdutyaccount.repositories.UserFulfilledObligationsRepository
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.{Clock, LocalDate, Month, YearMonth}

class ObligationDetailsIntegrationSpec extends ISpecBase {
  protected val endpointName = "obligation"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .configure(Map("mongodb.uri" -> "mongodb://localhost:27017/test-alcohol-duty-account"))
      .overrides(bind(classOf[Clock]).toInstance(clock2025))
      .build()

  lazy val repository = app.injector.instanceOf[UserFulfilledObligationsRepository]

  override def beforeEach(): Unit = {
    await(repository.deleteAll())
    super.beforeEach()
  }
  override def afterEach(): Unit = {
    await(repository.deleteAll())
    super.afterEach()
  }

  "the open obligation details for period endpoint must" - {
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

    "respond with NOT_FOUND if no open obligation data" in new SetUp {
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

  "the open obligations endpoint must" - {
    "respond with OK if able to fetch data" in new SetUp {
      stubAuthorised(appaId)

      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(obligationDataSingleOpen).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getOpenObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(Seq(adrObligationDetails))
      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getOpenObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(
        ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data")
      )

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with no obligations if no obligation data" in new SetUp {
      stubAuthorised(appaId)

      stubGetWithParameters(url, expectedQueryParamsOpen, NOT_FOUND, notFoundErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getOpenObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(Seq.empty[AdrObligationData])

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the obligation api call" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsOpen, BAD_GATEWAY, otherErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getOpenObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsOpen, expectedHeaders)
    }
  }

  "the fulfilled obligations endpoint must" - {
    "respond with OK if able to fetch fulfilled obligations from the cache" in new SetUp {
      await(repository.set(userFulfilledObligations))

      stubAuthorised(appaId)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getFulfilledObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(fulfilledObligationsData)

      verifyGetWithParametersNeverCalled(url, expectedQueryParamsFulfilled(2024))
      verifyGetWithParametersNeverCalled(url, expectedQueryParamsFulfilled(2025))
    }

    "respond with OK if able to fetch fulfilled obligations from the connector" in new SetUp {
      val obligationData2025 = ObligationData(
        obligations = Seq(
          Obligation(
            obligationDetails = Seq(
              ObligationDetails(
                status = Fulfilled,
                inboundCorrespondenceFromDate = LocalDate.of(2025, 1, 1),
                inboundCorrespondenceToDate = LocalDate.of(2025, 1, 31),
                inboundCorrespondenceDateReceived = None,
                inboundCorrespondenceDueDate = LocalDate.of(2025, 2, 15),
                periodKey = "25AA"
              )
            )
          )
        )
      )

      val expectedResponse = Seq(
        FulfilledObligations(2024, adrMultipleFulfilledData),
        FulfilledObligations(2025, Seq(fulfilledObligation(YearMonth.of(2025, Month.JANUARY))))
      )

      stubAuthorised(appaId)
      stubGetWithParameters(
        url,
        expectedQueryParamsFulfilled(2024),
        OK,
        Json.toJson(obligationDataMultipleFulfilled).toString
      )
      stubGetWithParameters(url, expectedQueryParamsFulfilled(2025), OK, Json.toJson(obligationData2025).toString)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getFulfilledObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(expectedResponse)

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2024), expectedHeaders)
      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2025), expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved from the connector cannot be parsed" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(
        url,
        expectedQueryParamsFulfilled(2024),
        OK,
        Json.toJson(obligationDataMultipleFulfilled).toString
      )
      stubGetWithParameters(url, expectedQueryParamsFulfilled(2025), OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getFulfilledObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(
        ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data")
      )

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2024), expectedHeaders)
      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2025), expectedHeaders)
    }

    "respond with empty lists if no fulfilled obligations" in new SetUp {
      val expectedResponse = Seq(FulfilledObligations(2024, Seq.empty), FulfilledObligations(2025, Seq.empty))

      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsFulfilled(2024), NOT_FOUND, "")
      stubGetWithParameters(url, expectedQueryParamsFulfilled(2025), NOT_FOUND, "")

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getFulfilledObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(expectedResponse)

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2024), expectedHeaders)
      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2025), expectedHeaders)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the obligation api call" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, expectedQueryParamsFulfilled(2024), GATEWAY_TIMEOUT, otherErrorMessage)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.getFulfilledObligations(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))

      verifyGetWithParametersAndHeaders(url, expectedQueryParamsFulfilled(2024), expectedHeaders)
      verifyGetWithParametersNeverCalled(url, expectedQueryParamsFulfilled(2025))
    }
  }

  class SetUp {

    val expectedQueryParamsOpen = Seq("status" -> Open.value)

    def expectedQueryParamsFulfilled(year: Int): Seq[(String, String)] =
      Seq("status" -> Fulfilled.value, "from" -> s"$year-01-01", "to" -> s"$year-12-31")

    val expectedHeaders                                                = Seq(
      HeaderNames.authorisation -> s"Bearer ${config.obligationDataToken}",
      "Environment"             -> config.obligationDataEnv
    )

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

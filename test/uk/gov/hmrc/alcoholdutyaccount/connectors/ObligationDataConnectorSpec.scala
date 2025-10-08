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

package uk.gov.hmrc.alcoholdutyaccount.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Fulfilled, Open}
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

class ObligationDataConnectorSpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "obligation"

  "ObligationDataConnector" - {
    "successfully get open obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(obligationDataSingleOpen).toString)
      whenReady(connector.getOpenObligations(appaId)) { result =>
        result mustBe Right(obligationDataSingleOpen)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "successfully get fulfilled obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsFulfilled, OK, Json.toJson(obligationDataSingleFulfilled).toString)
      whenReady(connector.getFulfilledObligations(appaId, year)) { result =>
        result mustBe Right(obligationDataSingleFulfilled)
        verifyGetWithParameters(url, expectedQueryParamsFulfilled)
      }
    }

    "successfully filter out future open obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(openObligationDataFromFuture).toString)
      whenReady(connector.getOpenObligations(appaId)) { result =>
        result mustBe Right(noObligations)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "successfully filter out future fulfilled obligation data" in new SetUp {
      stubGetWithParameters(
        url,
        expectedQueryParamsFulfilled,
        OK,
        Json.toJson(fulfilledObligationDataFromFuture).toString
      )
      whenReady(connector.getFulfilledObligations(appaId, year)) { result =>
        result mustBe Right(noObligations)
        verifyGetWithParameters(url, expectedQueryParamsFulfilled)
      }
    }

    "successfully filter out open obligations which are not due yet (because the correspondence to date is today)" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(openObligationDataFromTomorrow).toString)
      whenReady(connector.getOpenObligations(appaId)) { result =>
        result mustBe Right(noObligations)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "successfully filter out fulfilled obligations which are not due yet (because the correspondence to date is today)" in new SetUp {
      stubGetWithParameters(
        url,
        expectedQueryParamsFulfilled,
        OK,
        Json.toJson(fulfilledObligationDataFromTomorrow).toString
      )
      whenReady(connector.getFulfilledObligations(appaId, year)) { result =>
        result mustBe Right(noObligations)
        verifyGetWithParameters(url, expectedQueryParamsFulfilled)
      }
    }

    "NOT filter out open obligation due from today (because the correspondence to date was yesterday)" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(openObligationDataFromToday).toString)
      whenReady(connector.getOpenObligations(appaId)) { result =>
        result mustBe Right(openObligationDataFromToday)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "NOT filter out fulfilled obligation due from today (because the correspondence to date was yesterday)" in new SetUp {
      stubGetWithParameters(
        url,
        expectedQueryParamsFulfilled,
        OK,
        Json.toJson(fulfilledObligationDataFromToday).toString
      )
      whenReady(connector.getFulfilledObligations(appaId, year)) { result =>
        result mustBe Right(fulfilledObligationDataFromToday)
        verifyGetWithParameters(url, expectedQueryParamsFulfilled)
      }
    }

    "return an INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, "blah")
      whenReady(connector.getOpenObligations(appaId)) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "return no obligations if obligation data object cannot be found" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, NOT_FOUND, notFoundErrorMessage)
      whenReady(connector.getOpenObligations(appaId)) { result =>
        result mustBe Right(noObligations)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "return BAD_REQUEST if a bad request received with no retry" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, BAD_REQUEST, otherErrorMessage)
      whenReady(connectorWithRetry.getOpenObligations(appaId)) { result =>
        result mustBe Left(ErrorResponse(BAD_REQUEST, "Bad request"))
        verifyGetWithParametersWithoutRetry(url, expectedQueryParamsOpen)
      }
    }

    "return UNPROCESSABLE_ENTITY if an unprocessable entity received with no retry" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, UNPROCESSABLE_ENTITY, otherErrorMessage)
      whenReady(connectorWithRetry.getOpenObligations(appaId)) { result =>
        result mustBe Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity"))
        verifyGetWithParametersWithoutRetry(url, expectedQueryParamsOpen)
      }
    }

    "return an INTERNAL_SERVER_ERROR error" - {
      "if INTERNAL_SERVER_ERROR is returned when fetching obligation data" in new SetUp {
        stubGetWithParameters(
          url,
          expectedQueryParamsOpen,
          INTERNAL_SERVER_ERROR,
          Json.toJson(internalServerError).toString
        )
        whenReady(connector.getOpenObligations(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGetWithParameters(url, expectedQueryParamsOpen)
        }
      }

      "if an error other than BAD_REQUEST, NOT_FOUND or UNPROCESSABLE_ENTITY is returned the connector will invoke a retry" in new SetUp {
        stubGetWithParameters(
          url,
          expectedQueryParamsOpen,
          INTERNAL_SERVER_ERROR,
          Json.toJson(internalServerError).toString
        )
        whenReady(connectorWithRetry.getOpenObligations(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGetWithParametersWithRetry(url, expectedQueryParamsOpen)
        }
      }

      "if an exception is thrown when fetching obligation data" in new SetUp {
        stubGetFaultWithParameters(url, expectedQueryParamsOpen)
        whenReady(connector.getOpenObligations(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGetWithParameters(url, expectedQueryParamsOpen)
        }
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val connector: ObligationDataConnector          = appWithHttpClientV2.injector.instanceOf[ObligationDataConnector]
    val connectorWithRetry: ObligationDataConnector =
      appWithHttpClientV2WithRetry.injector.instanceOf[ObligationDataConnector]

    val year = 2024

    private val dateFilterHeadersHeaders = Seq("from" -> s"$year-01-01", "to" -> s"$year-12-31")
    val expectedQueryParamsOpen          = Seq("status" -> Open.value)

    val expectedQueryParamsFulfilled =
      Seq("status" -> Fulfilled.value) ++ dateFilterHeadersHeaders

    val expectedQueryParamsNoStatus  = dateFilterHeadersHeaders

    val url = appConfig.obligationDataUrl(appaId)

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

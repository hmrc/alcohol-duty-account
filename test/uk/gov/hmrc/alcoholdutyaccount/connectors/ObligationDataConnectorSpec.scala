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

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Fulfilled, Open}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.{LocalDate, ZoneOffset}

class ObligationDataConnectorSpec extends SpecBase with ScalaFutures with ConnectorTestHelpers {
  protected val endpointName = "obligation"

  "ObligationDataConnector" - {
    "successfully get open obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, Json.toJson(obligationDataSingleOpen).toString)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilterOpen)).value) { result =>
        result mustBe Right(obligationDataSingleOpen)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "successfully get fulfilled obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsFulfilled, OK, Json.toJson(obligationDataSingleFulfilled).toString)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilterFulfilled)).value) { result =>
        result mustBe Right(obligationDataSingleFulfilled)
        verifyGetWithParameters(url, expectedQueryParamsFulfilled)
      }
    }
    "successfully get open and fulfilled obligation data if there is no filter" in new SetUp {
      stubGetWithParameters(
        url,
        expectedQueryParamsNoStatus,
        OK,
        Json.toJson(obligationDataMultipleOpenAndFulfilled).toString
      )
      whenReady(connector.getObligationDetails(appaId, None).value) { result =>
        result mustBe Right(obligationDataMultipleOpenAndFulfilled)
        verifyGetWithParameters(url, expectedQueryParamsNoStatus)
      }
    }

    "return an INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, OK, "blah")
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilterOpen)).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "return no obligations if obligation data object cannot be found" in new SetUp {
      stubGetWithParameters(url, expectedQueryParamsOpen, NOT_FOUND, notFoundErrorMessage)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilterOpen)).value) { result =>
        result mustBe Right(noObligations)
        verifyGetWithParameters(url, expectedQueryParamsOpen)
      }
    }

    "return an INTERNAL_SERVER_ERROR error" - {
      "if an http error other than NOT_FOUND when fetching obligation data" in new SetUp {
        stubGetWithParameters(url, expectedQueryParamsOpen, BAD_REQUEST, otherErrorMessage)
        whenReady(connector.getObligationDetails(appaId, Some(obligationFilterOpen)).value) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGetWithParameters(url, expectedQueryParamsOpen)
        }
      }
      "if an exception thrown when fetching obligation data" in new SetUp {
        stubGetFaultWithParameters(url, expectedQueryParamsOpen)
        whenReady(connector.getObligationDetails(appaId, Some(obligationFilterOpen)).value) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Connection reset by peer"))
          verifyGetWithParameters(url, expectedQueryParamsOpen)
        }
      }
    }
  }

  class SetUp extends ConnectorFixture with TestData {
    val connector                        = new ObligationDataConnector(config = config, httpClient = httpClient)
    private val dateFilterHeadersHeaders = Seq("from" -> "2023-09-01", "to" -> LocalDate.now(ZoneOffset.UTC).toString)
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

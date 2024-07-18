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
import uk.gov.hmrc.alcoholdutyaccount.models.hods.Open
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class ObligationDataConnectorSpec extends SpecBase with ScalaFutures with ConnectorTestHelpers {
  protected val endpointName = "obligation"

  "ObligationDataConnector" - {
    "successfully get open obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, OK, Json.toJson(obligationDataSingleOpen).toString)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilter)).value) { result =>
        result mustBe Right(obligationDataSingleOpen)
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "successfully get fulfilled obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, OK, Json.toJson(obligationDataSingleFulfilled).toString)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilter)).value) { result =>
        result mustBe Right(obligationDataSingleFulfilled)
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }
    "successfully get open and fulfilled obligation data if there is no filter" in new SetUp {
      stubGet(url, OK, Json.toJson(obligationDataMultipleOpenAndFulfilled).toString)
      whenReady(connector.getObligationDetails(appaId, None).value) { result =>
        result mustBe Right(obligationDataMultipleOpenAndFulfilled)
        verifyGet(url)
      }
    }

    "return an INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, OK, "blah")
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilter)).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "return not found if obligation data object cannot be found" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, NOT_FOUND, notFoundErrorMessage)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilter)).value) { result =>
        result mustBe Left(ErrorResponse(NOT_FOUND, "Obligation data not found"))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "return an INTERNAL_SERVER_ERROR error if an error other than NOT_FOUND when fetching obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, BAD_REQUEST, otherErrorMessage)
      whenReady(connector.getObligationDetails(appaId, Some(obligationFilter)).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }
  }

  class SetUp extends ConnectorFixture with TestData {
    val connector           = new ObligationDataConnector(config = config, httpClient = httpClient)
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

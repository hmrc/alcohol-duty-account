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

import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.models.ReturnPeriod
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Obligation, ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class ObligationDataConnectorSpec extends ConnectorBase {
  protected val endpointName = "obligation"

  "ObligationDataConnector" - {
    "successfully get open obligation data when no period key specified" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, OK, Json.toJson(obligationData).toString)
      whenReady(connector.getOpenObligationDetails(alcoholDutyReference).value) { result =>
        result mustBe Right(obligationData)
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "return not found if obligation data object cannot be found for the period key with no server error message" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, NOT_FOUND, "")
      whenReady(connector.getOpenObligationDetails(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(NOT_FOUND, ""))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "return not found if obligation data object cannot be found for the period key" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, NOT_FOUND, notFoundErrorMessage)
      whenReady(connector.getOpenObligationDetails(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(NOT_FOUND, ""))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "return an INTERNAL_SERVER_ERROR error if an error other than NOT_FOUND when fetching obligation data with no server error message" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, BAD_REQUEST, "")
      whenReady(connector.getOpenObligationDetails(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }

    "return an INTERNAL_SERVER_ERROR error if an error other than NOT_FOUND when fetching obligation data" in new SetUp {
      stubGetWithParameters(url, expectedQueryParams, BAD_REQUEST, otherErrorMessage)
      whenReady(connector.getOpenObligationDetails(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
        verifyGetWithParameters(url, expectedQueryParams)
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val connector            = new ObligationDataConnector(config = config, httpClient = httpClient)
    val idType               = config.obligationIdType
    val alcoholDutyReference = "XMADP0000000200"
    val regimeType           = config.obligationRegimeType
    val periodStart          = LocalDate.of(2023, 1, 1)
    val periodEnd            = LocalDate.of(2023, 1, 31)
    val periodKey            = "24AE"
    val returnPeriod         = ReturnPeriod(periodKey, 2024, 5)
    val expectedQueryParams  = Map(
      "status" -> Open.value
    )
    val url                  = s"${config.obligationDataApiUrl}/enterprise/obligation-data/$idType/$alcoholDutyReference/$regimeType"

    val obligationData = ObligationData(obligations =
      Seq(
        Obligation(
          obligationDetails = Seq(
            ObligationDetails(
              status = Open,
              inboundCorrespondenceFromDate = periodStart,
              inboundCorrespondenceToDate = periodEnd,
              inboundCorrespondenceDateReceived = None,
              inboundCorrespondenceDueDate = LocalDate.now().plusDays(1),
              periodKey = periodKey
            )
          )
        )
      )
    )

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

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
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class FinancialDataConnectorSpec extends SpecBase with ScalaFutures with ConnectorTestHelpers {
  protected val endpointName = "financial"

  "FinancialDataConnector" - {
    "when calling getFinancialData" - {
      "returns the financial transaction document if the request is successful when requesting open transactions" in new SetUp {
        stubGetWithParameters(
          url,
          expectedOpenQueryParams,
          OK,
          Json.toJson(financialDocumentWithSingleSapDocumentNo).toString
        )
        whenReady(connector.getOnlyOpenFinancialData(appaId)) { result =>
          result mustBe Right(financialDocumentWithSingleSapDocumentNo)
          verifyGetWithParameters(url, expectedOpenQueryParams)
        }
      }

      "returns the financial transaction document if the request is successful when requesting all transactions" in new SetUp {
        stubGetWithParameters(
          url,
          expectedAllQueryParams,
          OK,
          Json.toJson(financialDocumentWithSingleSapDocumentNo).toString
        )
        whenReady(connector.getNotOnlyOpenFinancialData(appaId)) { result =>
          result mustBe Right(financialDocumentWithSingleSapDocumentNo)
          verifyGetWithParameters(url, expectedAllQueryParams)
        }
      }

      "returns an empty document if the financial transaction document cannot be found" in new SetUp {
        stubGetWithParameters(url, expectedOpenQueryParams, NOT_FOUND, "")
        whenReady(connector.getOnlyOpenFinancialData(appaId)) { result =>
          result mustBe Right(emptyFinancialDocument)
          verifyGetWithParameters(url, expectedOpenQueryParams)
        }
      }

      "return an error" - {
        "if the data retrieved cannot be parsed" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, OK, "blah")
          whenReady(connector.getOnlyOpenFinancialData(appaId)) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }

        "if the api call returns BAD_REQUEST with no retry" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, BAD_REQUEST, "")
          whenReady(connectorWithRetry.getOnlyOpenFinancialData(appaId)) { result =>
            result mustBe Left(ErrorCodes.badRequest)
            verifyGetWithParametersWithoutRetry(url, expectedOpenQueryParams)
          }
        }

        "if the api call returns UNPROCESSABLE_ENTITY with no retry" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, UNPROCESSABLE_ENTITY, "")
          whenReady(connectorWithRetry.getOnlyOpenFinancialData(appaId)) { result =>
            result mustBe Left(ErrorCodes.unprocessableEntity)
            verifyGetWithParametersWithoutRetry(url, expectedOpenQueryParams)
          }
        }

        "if an error other than BAD_REQUEST or NOT_FOUND is returned the connector will invoke a retry" in new SetUp {
          stubGetWithParameters(
            url,
            expectedOpenQueryParams,
            INTERNAL_SERVER_ERROR,
            Json.toJson(internalServerError).toString
          )
          whenReady(connectorWithRetry.getOnlyOpenFinancialData(appaId)) { result =>
            result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))
            verifyGetWithParametersWithRetry(url, expectedOpenQueryParams)
          }
        }

        "if the api call returns INTERNAL_SERVER_ERROR" in new SetUp {
          stubGetWithParameters(
            url,
            expectedOpenQueryParams,
            INTERNAL_SERVER_ERROR,
            Json.toJson(internalServerError).toString
          )
          whenReady(connector.getOnlyOpenFinancialData(appaId)) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }

        "if an exception is thrown when fetching financial data" in new SetUp {
          stubGetFaultWithParameters(url, expectedOpenQueryParams)
          whenReady(connector.getOnlyOpenFinancialData(appaId)) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }
      }
    }
  }

  class SetUp extends ConnectorFixture with TestData {
    val connector: FinancialDataConnector          = appWithHttpClientV2.injector.instanceOf[FinancialDataConnector]
    val connectorWithRetry: FinancialDataConnector =
      appWithHttpClientV2WithRetry.injector.instanceOf[FinancialDataConnector]

    val url: String = appConfig.financialDataUrl(appaId)
    val year: Int   = 2024

    val expectedOpenQueryParams: Seq[(String, String)] = Seq(
      "onlyOpenItems"              -> "true",
      "includeLocks"               -> "false",
      "calculateAccruedInterest"   -> "false",
      "customerPaymentInformation" -> "false"
    )

    val expectedAllQueryParams: Seq[(String, String)] = Seq(
      "onlyOpenItems"              -> "false",
      "includeLocks"               -> "false",
      "calculateAccruedInterest"   -> "false",
      "customerPaymentInformation" -> "false",
      "dateFrom"                   -> "2024-10-31",
      "dateTo"                     -> "2025-10-30"
    )
  }
}

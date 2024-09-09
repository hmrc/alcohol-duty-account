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
        whenReady(connector.getOnlyOpenFinancialData(appaId).value) { result =>
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
        whenReady(connector.getNotOnlyOpenFinancialData(appaId, year).value) { result =>
          result mustBe Right(financialDocumentWithSingleSapDocumentNo)
          verifyGetWithParameters(url, expectedAllQueryParams)
        }
      }

      "return an error" - {
        "if the data retrieved cannot be parsed" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, OK, "blah")
          whenReady(connector.getOnlyOpenFinancialData(appaId).value) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }

        "if the financial transaction document cannot be found" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, NOT_FOUND, "")
          whenReady(connector.getOnlyOpenFinancialData(appaId).value) { result =>
            result mustBe Left(ErrorCodes.entityNotFound)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }

        "if the api call returns BAD_REQUEST" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, BAD_REQUEST, "")
          whenReady(connector.getOnlyOpenFinancialData(appaId).value) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }

        "if the api call returns INTERNAL_SERVER_ERROR" in new SetUp {
          stubGetWithParameters(url, expectedOpenQueryParams, INTERNAL_SERVER_ERROR, "")
          whenReady(connector.getOnlyOpenFinancialData(appaId).value) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }

        "if an exception is thrown when fetching financial data" in new SetUp {
          stubGetFaultWithParameters(url, expectedOpenQueryParams)
          whenReady(connector.getOnlyOpenFinancialData(appaId).value) { result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)
            verifyGetWithParameters(url, expectedOpenQueryParams)
          }
        }
      }
    }
  }

  class SetUp extends ConnectorFixture with TestData {
    val connector = new FinancialDataConnector(config = config, httpClient = httpClient)
    val url       = appConfig.financialDataUrl(appaId)

    val year: Int = 2024

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
      "dateFrom"                   -> s"$year-01-01",
      "dateTo"                     -> s"$year-12-31"
    )
  }
}

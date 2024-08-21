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
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class FinancialDataConnectorSpec extends SpecBase with ScalaFutures with ConnectorTestHelpers {
  protected val endpointName = "financial"

  "FinancialDataConnector" - {
    "when calling getFinancialData" - {
      "returns the financial transaction document if the request is successful" in new SetUp {
        stubGetWithParameters(
          url,
          expectedQueryParams,
          OK,
          Json.toJson(financialDocumentWithSingleSapDocumentNo).toString
        )
        whenReady(connector.getFinancialData(appaId).value) { result =>
          result mustBe Right(financialDocumentWithSingleSapDocumentNo)
          verifyGetWithParameters(url, expectedQueryParams)
        }
      }

      "return an error" - {
        "if the data retrieved cannot be parsed" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, OK, "blah")
          whenReady(connector.getFinancialData(appaId).value) { result =>
            result mustBe Left(
              ErrorResponse(
                INTERNAL_SERVER_ERROR,
                s"Parsing failed for financial transaction document for appaId $appaId"
              )
            )
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if the financial transaction document cannot be found" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, NOT_FOUND, "")
          whenReady(connector.getFinancialData(appaId).value) { result =>
            result mustBe Left(ErrorResponse(NOT_FOUND, s"No financial data found for appaId $appaId"))
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if the api call returns BAD_REQUEST" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, BAD_REQUEST, "")
          whenReady(connector.getFinancialData(appaId).value) { result =>
            result mustBe Left(
              ErrorResponse(INTERNAL_SERVER_ERROR, s"Bad request when fetching financial data for appaId $appaId")
            )
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if the api call returns INTERNAL_SERVER_ERROR" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, INTERNAL_SERVER_ERROR, "")
          whenReady(connector.getFinancialData(appaId).value) { result =>
            result mustBe Left(
              ErrorResponse(
                INTERNAL_SERVER_ERROR,
                s"An error was returned while trying to fetch financial transaction document appaId $appaId"
              )
            )
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if an exception is thrown when fetching financial data" in new SetUp {
          stubGetFaultWithParameters(url, expectedQueryParams)
          whenReady(connector.getFinancialData(appaId).value) { result =>
            result mustBe Left(
              ErrorResponse(
                INTERNAL_SERVER_ERROR,
                s"An exception was returned while trying to fetch financial data for appaId $appaId"
              )
            )
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }
      }
    }

    "when calling getFinancialDataForBtaTile" - {
      "returns the financial transaction document if the request is successful" in new SetUp {
        stubGetWithParameters(
          url,
          expectedQueryParams,
          OK,
          Json.toJson(financialDocumentWithSingleSapDocumentNo).toString
        )
        whenReady(connector.getFinancialDataForBtaTile(appaId).value) { result =>
          result mustBe Some(financialDocumentWithSingleSapDocumentNo)
          verifyGetWithParameters(url, expectedQueryParams)
        }
      }

      "return an empty document" - {
        "if the financial transaction document cannot be found" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, NOT_FOUND, "")
          whenReady(connector.getFinancialDataForBtaTile(appaId).value) { result =>
            result mustBe Some(emptyFinancialDocument)
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }
      }

      "return None " - {
        "if the data retrieved cannot be parsed" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, OK, "blah")
          whenReady(connector.getFinancialDataForBtaTile(appaId).value) { result =>
            result mustBe None
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if the api call returns BAD_REQUEST" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, BAD_REQUEST, "")
          whenReady(connector.getFinancialDataForBtaTile(appaId).value) { result =>
            result mustBe None
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if the api call returns INTERNAL_SERVER_ERROR" in new SetUp {
          stubGetWithParameters(url, expectedQueryParams, INTERNAL_SERVER_ERROR, "")
          whenReady(connector.getFinancialDataForBtaTile(appaId).value) { result =>
            result mustBe None
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }

        "if an exception thrown when fetching financial data" in new SetUp {
          stubGetFaultWithParameters(url, expectedQueryParams)
          whenReady(connector.getFinancialDataForBtaTile(appaId).value) { result =>
            result mustBe None
            verifyGetWithParameters(url, expectedQueryParams)
          }
        }
      }
    }
  }

  class SetUp extends ConnectorFixture with TestData {
    val connector                                  = new FinancialDataConnector(config = config, httpClient = httpClient)
    val url                                        = appConfig.financialDataUrl(appaId)
    val expectedQueryParams: Seq[(String, String)] = Seq(
      "onlyOpenItems"              -> "true",
      "includeLocks"               -> "false",
      "calculateAccruedInterest"   -> "false",
      "customerPaymentInformation" -> "false"
    )
  }
}

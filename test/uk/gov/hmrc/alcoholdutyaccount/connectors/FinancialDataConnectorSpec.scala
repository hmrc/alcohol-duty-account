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

import org.mockito.ArgumentMatchersSugar.*
import org.mockito.MockitoSugar.mock
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionDocument, FinancialTransactionItem}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class FinancialDataConnectorSpec extends SpecBase {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConfig: AppConfig  = mock[AppConfig]
  val httpClient: HttpClient = mock[HttpClient]

  val connector = new FinancialDataConnector(config = mockConfig, httpClient = httpClient)

  "FinancialDataConnector" - {
    "successfully retrieves financial document object" in {

      val expectedFinancialData = FinancialTransactionDocument(
        financialTransactions = Seq(
          FinancialTransaction(
            periodKey = "18AA",
            chargeReference = "XM002610011594",
            originalAmount = 100.00,
            outstandingAmount = 50.00,
            mainTransaction = "1234",
            subTransaction = "5678",
            items = Seq(
              FinancialTransactionItem(
                subItem = "001",
                paymentAmount = 50.00
              )
            )
          )
        )
      )

      val json =
        """
              |{
              |  "financialTransactions": [{
              |    "periodKey": "18AA",
              |    "chargeReference": "XM002610011594",
              |    "originalAmount": 100.00,
              |    "outstandingAmount": 50.00,
              |    "mainTransaction": "1234",
              |    "subTransaction": "5678",
              |    "items": [{
              |      "subItem": "001",
              |      "amount": 100.00,
              |      "paymentAmount": 50.00
              |      }]
              |    }]
              |}
              |""".stripMargin

      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](*, *, *)(*, *, *) returnsF Right(
        HttpResponse(OK, json)
      )

      whenReady(connector.getFinancialData("ID001").value) { result =>
        result shouldBe Some(expectedFinancialData)
      }

    }

    "return None if the response has a status different from 200" in {
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](*, *, *)(*, *, *) returnsF Right(
        HttpResponse(NOT_FOUND, "{}")
      )

      whenReady(connector.getFinancialData("ID001").value) { result =>
        result shouldBe None
      }
    }
  }

}

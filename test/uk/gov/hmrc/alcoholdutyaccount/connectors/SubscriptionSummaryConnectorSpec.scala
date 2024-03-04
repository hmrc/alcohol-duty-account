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
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import org.mockito.MockitoSugar.mock
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class SubscriptionSummaryConnectorSpec extends SpecBase {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConfig: AppConfig                    = mock[AppConfig]
  val httpClient: HttpClient                   = mock[HttpClient]
  val connector                                = new SubscriptionSummaryConnector(config = mockConfig, httpClient = httpClient)
  val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(
      Beer,
      CiderOrPerry,
      WineAndOtherFermentedProduct,
      Spirits
    ),
    smallCiderFlag = false,
    approvalStatus = hods.Approved,
    insolvencyFlag = false
  )

  "SubscriptionSummaryConnector" - {
    "successfully retrieves a subscription summary" in {

      val json = """
          |{
          |  "typeOfAlcoholApprovedForList": ["01", "02", "03", "04"],
          |  "smallCiderFlag": "0",
          |  "approvalStatus": "01",
          |  "insolvencyFlag": "0"
          |}
          |""".stripMargin

      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](*, *, *)(*, *, *) returnsF Right(
        HttpResponse(200, json)
      )

      whenReady(connector.getSubscriptionSummary("ID001").value) { result =>
        result shouldBe Some(subscriptionSummary)
      }
    }

    "return None if the response has a status different from 200" in {
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](*, *, *)(*, *, *) returnsF Right(
        HttpResponse(404, "{}")
      )

      whenReady(connector.getSubscriptionSummary("ID001").value) { result =>
        result shouldBe None
      }
    }
  }
}

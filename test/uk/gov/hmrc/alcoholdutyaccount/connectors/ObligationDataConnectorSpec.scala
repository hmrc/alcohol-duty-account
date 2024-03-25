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
import uk.gov.hmrc.alcoholdutyaccount.models.hods.ObligationData
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class ObligationDataConnectorSpec extends SpecBase {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConfig: AppConfig  = mock[AppConfig]
  val httpClient: HttpClient = mock[HttpClient]
  val connector              = new ObligationDataConnector(config = mockConfig, httpClient = httpClient)

  val obligationData = ObligationData(
    obligations = Seq.empty
  )

  "ObligationDataConnector" - {
    "successfully retrieves an obligation data object" in {
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](*, *, *)(*, *, *) returnsF Right(
        HttpResponse(OK, """{"obligations":[]}""")
      )

      whenReady(connector.getObligationData("ID001").value) { result =>
        result shouldBe Some(obligationData)
      }
    }

    "return None if the response has a status different from 200" in {
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](*, *, *)(*, *, *) returnsF Right(
        HttpResponse(NOT_FOUND, "{}")
      )

      whenReady(connector.getObligationData("ID001").value) { result =>
        result shouldBe None
      }
    }
  }

}

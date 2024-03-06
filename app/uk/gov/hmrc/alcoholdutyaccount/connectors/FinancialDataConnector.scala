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

import cats.data.OptionT
import play.api.http.Status.OK
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.Document
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReadsInstances, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject() (config: AppConfig, implicit val httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends HttpReadsInstances {

  def getFinancialData(alcoholDutyReference: String)(implicit hc: HeaderCarrier): OptionT[Future, Document] =
    OptionT {
      val url = s"${config.financialDataApiUrl}/enterprise/financial-data/ZAD/$alcoholDutyReference/AD"
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](url = url).map {
        case Right(response) if response.status == OK => response.json.asOpt[Document]
        case _                                        => None
      }
    }
}

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
import uk.gov.hmrc.alcoholdutyaccount.models.hods.ObligationData
import uk.gov.hmrc.http.{HttpClient, _}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ObligationDataConnector @Inject() (
  config: AppConfig,
  implicit val httpClient: HttpClient
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances {

  def getObligationData(alcoholDutyReference: String)(implicit hc: HeaderCarrier): OptionT[Future, ObligationData] =
    OptionT {
      httpClient
        .GET[Either[UpstreamErrorResponse, HttpResponse]](url = config.obligationDataApiUrl(alcoholDutyReference))
        .map {
          case Right(response) if response.status == OK => response.json.asOpt[ObligationData]
          case _                                        => None
        }
    }
}

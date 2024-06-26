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
import play.api.{Logger, Logging}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReadsInstances, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class FinancialDataConnector @Inject() (config: AppConfig, implicit val httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends HttpReadsInstances
    with Logging {
  override protected val logger: Logger = Logger(this.getClass)

  def getFinancialData(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): OptionT[Future, FinancialTransactionDocument] =
    OptionT {
      val url =
        s"${config.financialDataApiUrl}/enterprise/financial-data/${config.idType}/$alcoholDutyReference/${config.regimeType}"

      logger.info(s"Fetching financial transaction document for appaId $alcoholDutyReference")

      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](url = url).map {
        case Right(response) =>
          Try {
            response.json
              .asOpt[FinancialTransactionDocument]
          }.toOption.flatten
            .fold[Option[FinancialTransactionDocument]] {
              logger.warn(s"Unable to parse financial transaction document for appaId $alcoholDutyReference")
              None
            } {
              logger.info(s"Retrieved financial transaction document for appaId $alcoholDutyReference")
              Some(_)
            }
        case Left(error)     =>
          logger.warn(
            s"An error was returned while trying to fetch financial transaction document appaId $alcoholDutyReference: ${error.message}"
          )
          None
      }
    }
}

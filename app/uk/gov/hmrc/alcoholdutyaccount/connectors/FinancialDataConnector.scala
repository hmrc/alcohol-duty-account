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
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, HttpReadsInstances, HttpResponse, UpstreamErrorResponse}

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

      val headers: Seq[(String, String)] = Seq(
        HeaderNames.authorisation -> config.obligationDataToken,
        "Environment"             -> config.obligationDataEnv
      )

      val url =
        s"${config.financialDataHost}/enterprise/financial-data/${config.idType}/$alcoholDutyReference/${config.regime}"

      logger.info(s"Fetching financial transaction document for appaId $alcoholDutyReference")

      httpClient
        .GET[Either[UpstreamErrorResponse, HttpResponse]](
          url = url,
          queryParams = getQueryParams(),
          headers = headers
        )
        .map {
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
        .recoverWith { case e: Exception =>
          logger.warn(
            s"An exception was returned while trying to fetch financial data appaId $alcoholDutyReference: ${e.getMessage}"
          )
          Future.successful(None)
        }
    }

  private def getQueryParams(): Seq[(String, String)] =
    Seq(
      "onlyOpenItems"              -> true.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString
    )

}

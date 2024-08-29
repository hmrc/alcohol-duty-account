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

import cats.data.EitherT
import play.api.http.Status.NOT_FOUND
import play.api.{Logger, Logging}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, HttpReadsInstances, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FinancialDataConnector @Inject() (config: AppConfig, implicit val httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends HttpReadsInstances
    with Logging {
  override protected val logger: Logger = Logger(this.getClass)

  def getFinancialData(
    appaId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, FinancialTransactionDocument] =
    EitherT {
      val headers: Seq[(String, String)] = Seq(
        HeaderNames.authorisation -> s"Bearer ${config.financialDataToken}",
        "Environment"             -> config.financialDataEnv
      )

      logger.info(s"Fetching financial transaction document for appaId $appaId")

      httpClient
        .GET[Either[UpstreamErrorResponse, HttpResponse]](
          url = config.financialDataUrl(appaId),
          queryParams = getQueryParams,
          headers = headers
        )
        .map {
          case Right(response) =>
            Try {
              response.json
                .as[FinancialTransactionDocument]
            } match {
              case Success(doc)       =>
                logger.info(s"Retrieved financial transaction document for appaId $appaId")
                Right(doc)
              case Failure(exception) =>
                logger.warn(s"Parsing failed for financial transaction document for appaId $appaId", exception)
                Left(ErrorCodes.unexpectedResponse)
            }
          case Left(error)     =>
            Left(processErrors(appaId, error))
        }
        .recoverWith { case e: Exception =>
          logger.warn(s"An exception was returned while trying to fetch financial data for appaId $appaId", e)
          Future.successful(Left(ErrorCodes.unexpectedResponse))
        }
    }

  private def processErrors(appaId: String, error: UpstreamErrorResponse): ErrorResponse =
    error.statusCode match {
      case NOT_FOUND =>
        logger.info(s"No financial data found for appaId $appaId")
        ErrorCodes.entityNotFound
      case status    =>
        logger.warn(
          s"An error status $status was returned while trying to fetch financial transaction document appaId $appaId",
          error
        )
        ErrorCodes.unexpectedResponse
    }

  private def getQueryParams: Seq[(String, String)] =
    Seq(
      "onlyOpenItems"              -> true.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString
    )
}

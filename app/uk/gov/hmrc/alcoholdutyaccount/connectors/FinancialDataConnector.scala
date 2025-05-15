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

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern.retry
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, UNPROCESSABLE_ENTITY}
import play.api.{Logger, Logging}
import uk.gov.hmrc.alcoholdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.alcoholdutyaccount.models.{ErrorCodes, HttpErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpReadsInstances, HttpResponse, InternalServerException, StringContextOps}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FinancialDataConnector @Inject() (
  config: AppConfig,
  implicit val system: ActorSystem,
  implicit val httpClient: HttpClientV2
)(implicit
  circuitBreakerProvider: CircuitBreakerProvider,
  ec: ExecutionContext
) extends HttpReadsInstances
    with Logging {
  override protected val logger: Logger = Logger(this.getClass)

  implicit val scheduler: Scheduler = system.scheduler

  def getOnlyOpenFinancialData(
    appaId: String
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, FinancialTransactionDocument]] =
    getFinancialData(appaId, getBaseQueryParams(true))

  def getNotOnlyOpenFinancialData(
    appaId: String
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, FinancialTransactionDocument]] =
    getFinancialData(appaId, getOnlyOpenItemsFalseParameters)

  private def getFinancialData(
    appaId: String,
    queryParams: Seq[(String, String)]
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, FinancialTransactionDocument]] =
    retry(
      () => call(appaId, queryParams),
      attempts = config.retryAttempts,
      delay = config.retryAttemptsDelay
    ).recoverWith { _ =>
      Future.successful(Left(ErrorCodes.unexpectedResponse))
    }

  private def call(
    appaId: String,
    queryParams: Seq[(String, String)]
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, FinancialTransactionDocument]] =
    circuitBreakerProvider.get().withCircuitBreaker {
      val headers: Seq[(String, String)] = Seq(
        HeaderNames.authorisation -> s"Bearer ${config.financialDataToken}",
        "Environment"             -> config.financialDataEnv
      )

      logger.info(s"Fetching financial transaction document for appaId $appaId")

      httpClient
        .get(url"${config.financialDataUrl(appaId)}?$queryParams")
        .setHeader(headers: _*)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK                   =>
              Try {
                response.json
                  .as[FinancialTransactionDocument]
              } match {
                case Success(doc)       =>
                  logger.info(s"Retrieved financial transaction document for appaId $appaId")
                  Future.successful(Right(doc))
                case Failure(exception) =>
                  logger.warn(s"Parsing failed for financial transaction document for appaId $appaId", exception)
                  Future.successful(Left(ErrorCodes.unexpectedResponse))
              }
            case NOT_FOUND            =>
              logger.info(s"No financial data found for appaId $appaId")
              Future.successful(Right(FinancialTransactionDocument.emptyDocument))
            case BAD_REQUEST          =>
              logger.warn(s"Bad request sent to get financial data for appaId $appaId")
              Future.successful(Left(ErrorResponse(BAD_REQUEST, "Bad request")))
            case UNPROCESSABLE_ENTITY =>
              logger.warn(s"Get financial data request unprocessable for appaId $appaId")
              Future.successful(Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity")))
            case _                    =>
              val error: String = response.json.as[HttpErrorResponse].message
              logger.warn(
                s"An exception was returned while trying to fetch financial data for appaId $appaId: $error"
              )
              Future.failed(new InternalServerException(response.body))
          }
        }
    }

  private def getBaseQueryParams(onlyOpenItems: Boolean): Seq[(String, String)] =
    Seq(
      "onlyOpenItems"              -> onlyOpenItems.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString
    )

  private def getOnlyOpenItemsFalseParameters: Seq[(String, String)] =
    getBaseQueryParams(false) ++ Seq(
      "dateFrom" -> config.historicDataStartDate,
      "dateTo"   -> config.historicDataEndDate
    )
}

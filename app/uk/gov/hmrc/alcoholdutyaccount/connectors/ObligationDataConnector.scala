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
import play.api.http.Status._
import play.api.{Logger, Logging}
import uk.gov.hmrc.alcoholdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.hods._
import uk.gov.hmrc.alcoholdutyaccount.utils.DateTimeHelper.instantToLocalDate
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ObligationDataConnector @Inject() (
  config: AppConfig,
  clock: Clock,
  circuitBreakerProvider: CircuitBreakerProvider,
  implicit val system: ActorSystem,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {
  override protected val logger: Logger = Logger(this.getClass)
  implicit val scheduler: Scheduler     = system.scheduler

  def getOpenObligations(appaId: String)(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, ObligationData]] =
    getObligationDetails(appaId, openQueryParams)

  def getFulfilledObligations(appaId: String, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, ObligationData]] =
    getObligationDetails(appaId, getFulfilledQueryParams(year))

  private def getObligationDetails(
    appaId: String,
    queryParams: Seq[(String, String)]
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, ObligationData]] =
    retry(
      () => call(appaId, queryParams),
      attempts = config.retryAttempts,
      delay = config.retryAttemptsDelay
    ).recoverWith { error =>
      logger.error(
        s"An exception was returned while trying to fetch obligation data for appaId $appaId: $error"
      )
      Future.successful(Left(ErrorCodes.unexpectedResponse))
    }

  def call(
    appaId: String,
    queryParams: Seq[(String, String)]
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, ObligationData]] =
    circuitBreakerProvider.get().withCircuitBreaker {
      val headers: Seq[(String, String)] = Seq(
        HeaderNames.authorisation -> s"Bearer ${config.obligationDataToken}",
        "Environment"             -> config.obligationDataEnv
      )
      logger.info(s"Fetching all open obligation data for appaId $appaId")

      httpClient
        .get(url"${config.obligationDataUrl(appaId)}?$queryParams")
        .setHeader(headers: _*)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK                   =>
              Try {
                response.json.as[ObligationData]
              } match {
                case Success(doc)       =>
                  logger.info(s"Retrieved obligation data for appaId $appaId")
                  Future.successful(Right(filterOutFutureObligations(doc)))
                case Failure(exception) =>
                  logger.error(s"Unable to parse obligation data for appaId $appaId", exception)
                  Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data")))
              }
            case NOT_FOUND            =>
              logger.info(s"No obligation data found for appaId $appaId")
              Future.successful(Right(ObligationData.noObligations))
            case BAD_REQUEST          =>
              logger.warn(s"Bad request sent to get obligation for appaId $appaId")
              Future.successful(Left(ErrorResponse(BAD_REQUEST, "Bad request")))
            case UNPROCESSABLE_ENTITY =>
              logger.warn(s"Obligation data request unprocessable for appaId $appaId")
              Future.successful(Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity")))
            case _                    =>
              // Retry - do not log until final fail
              Future.failed(new InternalServerException(response.body))
          }
        }
    }

  private val openQueryParams: Seq[(String, String)]                    =
    Seq("status" -> Open.value)

  private def getFulfilledQueryParams(year: Int): Seq[(String, String)] =
    Seq(
      "status" -> Fulfilled.value,
      "from"   -> s"$year-01-01",
      "to"     -> s"$year-12-31"
    )

  private def filterOutFutureObligations(obligationData: ObligationData): ObligationData =
    obligationData.copy(obligations = obligationData.obligations.flatMap { obligation =>
      val filteredDetails: Seq[ObligationDetails] = obligation.obligationDetails.filter { obligationDetail =>
        obligationDetail.inboundCorrespondenceToDate.isBefore(instantToLocalDate(Instant.now(clock)))
      }
      // This code makes empty obligations of format ObligationData(obligations = Seq.empty), matching how they are currently returned
      if (filteredDetails.nonEmpty) Some(obligation.copy(obligationDetails = filteredDetails)) else None
    })

}

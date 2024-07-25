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
import play.api.{Logger, Logging}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ObligationData, ObligationStatus, Open}
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.{LocalDate, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ObligationDataConnector @Inject() (
  config: AppConfig,
  implicit val httpClient: HttpClient
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {
  override protected val logger: Logger = Logger(this.getClass)

  def getObligationDetails(
    alcoholDutyReference: String,
    obligationStatusFilter: Option[ObligationStatus]
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, ObligationData] =
    EitherT {

      val headers: Seq[(String, String)] = Seq(
        HeaderNames.authorisation -> s"Bearer ${config.obligationDataToken}",
        "Environment"             -> config.obligationDataEnv
      )

      val url =
        s"${config.obligationDataHost}/enterprise/obligation-data/${config.idType}/$alcoholDutyReference/${config.regime}"
      logger.info(s"Fetching all open obligation data for appaId $alcoholDutyReference")

      httpClient
        .GET[Either[UpstreamErrorResponse, HttpResponse]](
          url = url,
          queryParams = getQueryParams(obligationStatusFilter),
          headers = headers
        )
        .map {
          case Right(response) =>
            Try {
              response.json
                .asOpt[ObligationData]
            }.toOption.flatten
              .fold[Either[ErrorResponse, ObligationData]] {
                logger.warn(s"Unable to parse obligation data for appaId $alcoholDutyReference")
                Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))
              } {
                logger.info(s"Retrieved open obligation data for appaId $alcoholDutyReference")
                Right(_)
              }
          case Left(error)     => Left(processError(error, alcoholDutyReference))
        }
        .recoverWith { case e: Exception =>
          logger.warn(
            s"An exception was returned while trying to fetch obligation data appaId $alcoholDutyReference: ${e.getMessage}"
          )
          Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage)))
        }
    }

  private def getQueryParams(obligationStatusFilter: Option[ObligationStatus]): Seq[(String, String)] = {
    // date filter headers should only be added if the status is not defined or is not Open (according to api specs)
    val dateFilterHeaders =
      Seq("from" -> config.obligationDataFilterStartDate, "to" -> LocalDate.now(ZoneId.of("Europe/London")).toString)
    obligationStatusFilter match {
      case Some(Open)   => Seq("status" -> Open.value)
      case Some(status) => Seq("status" -> status.value) ++ dateFilterHeaders
      case None         => dateFilterHeaders
    }
  }

  private def processError(error: UpstreamErrorResponse, alcoholDutyReference: String): ErrorResponse =
    error.statusCode match {
      case NOT_FOUND =>
        logger.info(s"No obligation data found for appaId $alcoholDutyReference")
        ErrorResponse(NOT_FOUND, "Obligation data not found")
      case _         =>
        logger.warn(
          s"An error was returned while trying to fetch obligation data appaId $alcoholDutyReference: ${error.message}"
        )
        ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")
    }
}

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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.{Logger, Logging}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ObligationData, ObligationDetails, ObligationStatus, Open}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.{LocalDate, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ObligationDataConnector @Inject() (
  config: AppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {
  override protected val logger: Logger = Logger(this.getClass)

  def getObligationDetails(
    appaId: String,
    obligationStatusFilter: Option[ObligationStatus]
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, ObligationData] =
    EitherT {

      val headers: Seq[(String, String)] = Seq(
        HeaderNames.authorisation -> s"Bearer ${config.obligationDataToken}",
        "Environment"             -> config.obligationDataEnv
      )

      logger.info(s"Fetching all open obligation data for appaId $appaId")

      val queryParams = getQueryParams(obligationStatusFilter)

      httpClient
        .get(url"${config.obligationDataUrl(appaId)}?$queryParams")
        .setHeader(headers: _*)
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Right(response) =>
            Try {
              response.json.as[ObligationData]
            } match {
              case Success(doc)       =>
                logger.info(s"Retrieved open obligation data for appaId $appaId")
                Right(filterOutFutureObligations(doc))
              case Failure(exception) =>
                logger.warn(s"Unable to parse obligation data for appaId $appaId", exception)
                Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse obligation data"))
            }
          case Left(error)
              if error.statusCode == NOT_FOUND => // This is not necessarily an error, just no obligations were returned
            logger.info(s"No obligation data found for appaId $appaId")
            Right(ObligationData.noObligations)
          case Left(error)     => Left(processError(error, appaId))
        }
        .recoverWith { case e: Exception =>
          logger.warn(
            s"An exception was returned while trying to fetch obligation data appaId $appaId",
            e
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

  private def processError(error: UpstreamErrorResponse, appaId: String): ErrorResponse = {
    logger.warn(
      s"An error was returned while trying to fetch obligation data appaId $appaId",
      error
    )

    ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")
  }

  private def filterOutFutureObligations(obligationData: ObligationData): ObligationData =
    obligationData.copy(obligations = obligationData.obligations.flatMap { obligation =>
      val filteredDetails: Seq[ObligationDetails] = obligation.obligationDetails.filter { obligationDetail =>
        obligationDetail.inboundCorrespondenceToDate.isBefore(LocalDate.now())
      }
      // This code makes empty obligations of format ObligationData(obligations = Seq.empty), matching how they are currently returned
      if (filteredDetails.nonEmpty) Some(obligation.copy(obligationDetails = filteredDetails)) else None
    })

}

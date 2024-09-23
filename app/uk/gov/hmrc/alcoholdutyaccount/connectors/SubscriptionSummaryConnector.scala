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
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{SubscriptionSummary, SubscriptionSummarySuccess}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionSummaryConnector @Inject() (
  config: AppConfig,
  headers: HIPHeaders,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getSubscriptionSummary(
    appaId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, SubscriptionSummary] =
    EitherT {

      logger.info(s"Fetching subscription summary for appaId $appaId")

      httpClient
        .get(url"${config.getSubscriptionUrl(appaId)}")
        .setHeader(headers.subscriptionHeaders(): _*)
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Right(response) =>
            Try {
              response.json
                .as[SubscriptionSummarySuccess]
            } match {
              case Success(doc)       =>
                logger.info(s"Retrieved subscription summary success for appaId $appaId")
                Right(doc.success)
              case Failure(exception) =>
                logger.warn(s"Unable to parse subscription summary success for appaId $appaId", exception)
                Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary success"))
            }
          case Left(error)     => Left(processError(error, appaId))
        }
        .recoverWith { case e: Exception =>
          logger.warn(
            s"An exception was returned while trying to fetch subscription summary appaId $appaId",
            e
          )
          Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage)))
        }
    }

  private def processError(error: UpstreamErrorResponse, appaId: String): ErrorResponse =
    error.statusCode match {
      case BAD_REQUEST =>
        logger.info(s"Bad request sent to get subscription for appaId $appaId", error)
        ErrorResponse(BAD_REQUEST, "Bad request")
      case NOT_FOUND   =>
        logger.info(s"No subscription summary found for appaId $appaId")
        ErrorResponse(NOT_FOUND, "Subscription summary not found")
      case _           =>
        logger.warn(
          s"An error was returned while trying to fetch subscription summary appaId $appaId",
          error
        )
        ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")
    }
}

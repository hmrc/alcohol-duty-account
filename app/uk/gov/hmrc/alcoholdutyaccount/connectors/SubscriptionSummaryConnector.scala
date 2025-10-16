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
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.alcoholdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.alcoholdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{SubscriptionSummary, SubscriptionSummarySuccess}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionSummaryConnector @Inject() (
  config: AppConfig,
  headers: HIPHeaders,
  circuitBreakerProvider: CircuitBreakerProvider,
  implicit val system: ActorSystem,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  implicit val scheduler: Scheduler = system.scheduler

  def getSubscriptionSummary(
    appaId: String
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SubscriptionSummary]] =
    retry(
      () => call(appaId),
      attempts = config.retryAttempts,
      delay = config.retryAttemptsDelay
    ).recoverWith { error =>
      logger.error(
        s"An exception was returned while trying to fetch subscription summary appaId $appaId: $error"
      )
      Future.successful(Left(ErrorCodes.unexpectedResponse))
    }

  def call(
    appaId: String
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SubscriptionSummary]] =
    circuitBreakerProvider.get().withCircuitBreaker {
      httpClient
        .get(url"${config.getSubscriptionUrl(appaId)}")
        .setHeader(headers.subscriptionHeaders(): _*)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK                   =>
              Try {
                response.json
                  .as[SubscriptionSummarySuccess]
              } match {
                case Success(doc) =>
                  logger.info(s"Retrieved subscription summary success for appaId $appaId")
                  Future.successful(Right(doc.success))
                case Failure(_)   =>
                  logger.error(s"Unable to parse subscription summary success for appaId $appaId")
                  Future
                    .successful(
                      Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary success"))
                    )
              }
            case BAD_REQUEST          =>
              logger.warn(s"Bad request sent to get subscription for appaId $appaId")
              Future.successful(Left(ErrorResponse(BAD_REQUEST, "Bad request")))
            case NOT_FOUND            =>
              logger.warn(s"No subscription summary found for appaId $appaId")
              Future.successful(Left(ErrorResponse(NOT_FOUND, "Subscription summary not found")))
            case UNPROCESSABLE_ENTITY =>
              logger.warn(s"Subscription summary request unprocessable for appaId $appaId")
              Future.successful(Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity")))
            case _                    =>
              // Retry - log on final fail
              Future.failed(new InternalServerException(response.body))
          }
        }
    }
}

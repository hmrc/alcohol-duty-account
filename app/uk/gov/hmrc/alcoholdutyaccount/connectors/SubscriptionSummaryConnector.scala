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

import org.apache.pekko.pattern.retry
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import uk.gov.hmrc.alcoholdutyaccount.config.{AppConfig, SubscriptionCircuitBreakerProvider}
import uk.gov.hmrc.alcoholdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{SubscriptionSummary, SubscriptionSummarySuccess}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

// TODO - Put debug statements back in
// TODO - Ensure retry mechanism set to 1 for spec tests
// TODO - Add one spec test that sets the retry > 1 and checks the API is called multiple times

class SubscriptionSummaryConnector @Inject() (
  config: AppConfig,
  headers: HIPHeaders,
  subscriptionCircuitBreakerProvider: SubscriptionCircuitBreakerProvider,
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
    ).map { response =>
      Right(response)
    }.recoverWith {
      case badRequest: BadRequestException     => Future.successful(Left(ErrorResponse(BAD_REQUEST, badRequest.getMessage)))
      case notFound: NotFoundException         => Future.successful(Left(ErrorResponse(NOT_FOUND, notFound.getMessage)))
      case illegalState: IllegalStateException =>
        Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, illegalState.getMessage)))
      case _                                   => Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")))
    }

  def call(
    appaId: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionSummary] =
    subscriptionCircuitBreakerProvider.get().withCircuitBreaker {
      httpClient
        .get(url"${config.getSubscriptionUrl(appaId)}")
        .setHeader(headers.subscriptionHeaders(): _*)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK          =>
              Try {
                response.json
                  .as[SubscriptionSummarySuccess]
              } match {
                case Success(doc) =>
                  Future.successful(doc.success)
                case Failure(_)   =>
                  Future
                    .failed(new IllegalStateException(s"Unable to parse subscription summary success"))
              }
            case BAD_REQUEST => Future.failed(new BadRequestException("Bad request"))
            case NOT_FOUND   => Future.failed(new NotFoundException("Subscription summary not found"))
            case _           => Future.failed(new InternalServerException("An error occurred"))
          }
        }
    }
}

/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.alcoholdutyaccount.service

import play.api.Logging
import uk.gov.hmrc.alcoholdutyaccount.models.{FulfilledObligations, UserFulfilledObligations}
import uk.gov.hmrc.alcoholdutyaccount.repositories.UserFulfilledObligationsRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FulfilledObligationsRepositoryService @Inject() (
  alcoholDutyService: AlcoholDutyService,
  userFulfilledObligationsRepository: UserFulfilledObligationsRepository,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends Logging {

  def getAllYearsFulfilledObligations(appaId: String, startYear: Int, endYear: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, Seq[FulfilledObligations]]] =
    userFulfilledObligationsRepository.get(appaId).flatMap {
      case Some(userFulfilledObligations) => Future.successful(Right(userFulfilledObligations.fulfilledObligationsData))
      case None                           =>
        val fulfilledObligationsFromApi: Future[Either[ErrorResponse, Seq[FulfilledObligations]]] =
          (startYear to endYear).foldLeft(
            Future.successful(Right(Seq.empty): Either[ErrorResponse, Seq[FulfilledObligations]])
          ) { case (accData, year) =>
            accData.flatMap {
              case Left(error)                       => Future.successful(Left(error))
              case Right(fulfilledObligationsByYear) =>
                alcoholDutyService.getFulfilledObligations(appaId, year).value.map {
                  case Left(e)                            => Left(e)
                  case Right(fulfilledObligationsForYear) =>
                    Right(fulfilledObligationsByYear :+ fulfilledObligationsForYear)
                }
            }
          }
        fulfilledObligationsFromApi.flatMap {
          case Left(error)                       => Future.successful(Left(error))
          case Right(fulfilledObligationsByYear) =>
            userFulfilledObligationsRepository
              .set(UserFulfilledObligations(appaId, fulfilledObligationsByYear, Instant.now(clock)))
              .map(_ => Right(fulfilledObligationsByYear))
        }
    }
}

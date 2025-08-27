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
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{HistoricPayments, UserHistoricPayments}
import uk.gov.hmrc.alcoholdutyaccount.repositories.UserHistoricPaymentsRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HistoricPaymentsRepositoryService @Inject() (
  historicPaymentsService: HistoricPaymentsService,
  userHistoricPaymentsRepository: UserHistoricPaymentsRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def getAllYearsHistoricPayments(appaId: String, startYear: Int, endYear: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, Seq[HistoricPayments]]] =
    userHistoricPaymentsRepository.get(appaId).flatMap {
      case Some(userHistoricPayments) => Future.successful(Right(userHistoricPayments.historicPaymentsData))
      case None                       =>
        val historicPaymentsFromApi: Future[Either[ErrorResponse, Seq[HistoricPayments]]] =
          (startYear to endYear).foldLeft(
            Future.successful(Right(Seq.empty): Either[ErrorResponse, Seq[HistoricPayments]])
          ) { case (accData, year) =>
            accData.flatMap {
              case Left(error)                   => Future.successful(Left(error))
              case Right(historicPaymentsByYear) =>
                historicPaymentsService.getHistoricPayments(appaId, year).value.map {
                  case Left(e)                        => Left(e)
                  case Right(historicPaymentsForYear) => Right(historicPaymentsByYear :+ historicPaymentsForYear)
                }
            }
          }
        historicPaymentsFromApi.flatMap {
          case Left(error)                   => Future.successful(Left(error))
          case Right(historicPaymentsByYear) =>
            userHistoricPaymentsRepository
              .set(UserHistoricPayments(appaId, historicPaymentsByYear))
              .map(_ => Right(historicPaymentsByYear))
        }
    }
}

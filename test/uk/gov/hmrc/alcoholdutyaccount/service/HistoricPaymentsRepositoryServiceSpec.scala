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

package uk.gov.hmrc.alcoholdutyaccount.service

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.payments.HistoricPayments
import uk.gov.hmrc.alcoholdutyaccount.repositories.UserHistoricPaymentsRepository
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.concurrent.Future

class HistoricPaymentsRepositoryServiceSpec extends SpecBase {
  "HistoricPaymentsRepositoryService" - {
    "when calling getAllYearsHistoricPayments" - {
      "must return the user's past payments if found in the cache" in new SetUp {
        when(mockRepository.get(eqTo(appaId))) thenReturn Future.successful(Some(userHistoricPayments))

        whenReady(historicPaymentsRepositoryService.getAllYearsHistoricPayments(appaId, startYear, endYear)) { result =>
          result mustBe Right(historicPaymentsData)

          verify(mockRepository, times(1)).get(eqTo(appaId))
          verify(mockRepository, times(0)).set(any())
          verify(mockHistoricPaymentsService, times(0)).getHistoricPayments(any(), any())(any())
        }
      }

      "must return the user's past payments obtained from the connector if not found in the cache" in new SetUp {
        when(mockRepository.get(eqTo(appaId))) thenReturn Future.successful(None)
        when(mockRepository.set(eqTo(userHistoricPayments))) thenReturn Future.successful(userHistoricPayments)

        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(2022))(any())) thenReturn EitherT
          .rightT[Future, HistoricPayments](historicPayments2022)
        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(2023))(any())) thenReturn EitherT
          .rightT[Future, HistoricPayments](historicPayments2023)
        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(2024))(any())) thenReturn EitherT
          .rightT[Future, HistoricPayments](historicPayments2024)
        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(2025))(any())) thenReturn EitherT
          .rightT[Future, HistoricPayments](historicPayments2025)

        whenReady(historicPaymentsRepositoryService.getAllYearsHistoricPayments(appaId, startYear, endYear)) { result =>
          result mustBe Right(historicPaymentsData)

          verify(mockRepository, times(1)).get(eqTo(appaId))
          verify(mockRepository, times(1)).set(eqTo(userHistoricPayments))
          verify(mockHistoricPaymentsService, times(1)).getHistoricPayments(eqTo(appaId), eqTo(2022))(any())
          verify(mockHistoricPaymentsService, times(1)).getHistoricPayments(eqTo(appaId), eqTo(2023))(any())
          verify(mockHistoricPaymentsService, times(1)).getHistoricPayments(eqTo(appaId), eqTo(2024))(any())
          verify(mockHistoricPaymentsService, times(1)).getHistoricPayments(eqTo(appaId), eqTo(2025))(any())
        }
      }

      "must return an ErrorResponse and not make subsequent calls if the connector returns an error" in new SetUp {
        when(mockRepository.get(eqTo(appaId))) thenReturn Future.successful(None)

        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(2022))(any())) thenReturn EitherT
          .rightT[Future, HistoricPayments](historicPayments2022)
        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(2023))(any())) thenReturn EitherT
          .leftT[Future, ErrorResponse](ErrorCodes.unexpectedResponse)

        whenReady(historicPaymentsRepositoryService.getAllYearsHistoricPayments(appaId, startYear, endYear)) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)

          verify(mockRepository, times(1)).get(eqTo(appaId))
          verify(mockRepository, times(0)).set(any())
          verify(mockHistoricPaymentsService, times(1)).getHistoricPayments(eqTo(appaId), eqTo(2022))(any())
          verify(mockHistoricPaymentsService, times(1)).getHistoricPayments(eqTo(appaId), eqTo(2023))(any())
          verify(mockHistoricPaymentsService, times(0)).getHistoricPayments(eqTo(appaId), eqTo(2024))(any())
          verify(mockHistoricPaymentsService, times(0)).getHistoricPayments(eqTo(appaId), eqTo(2025))(any())
        }
      }
    }
  }

  class SetUp {
    val mockHistoricPaymentsService: HistoricPaymentsService = mock[HistoricPaymentsService]
    val mockRepository: UserHistoricPaymentsRepository       = mock[UserHistoricPaymentsRepository]
    val historicPaymentsRepositoryService                    =
      new HistoricPaymentsRepositoryService(mockHistoricPaymentsService, mockRepository, clock)

    val startYear = 2022
    val endYear   = 2025
  }
}

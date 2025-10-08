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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.repositories.UserFulfilledObligationsRepository

import scala.concurrent.Future

class FulfilledObligationsRepositoryServiceSpec extends SpecBase {
  "FulfilledObligationsRepositoryService" - {
    "when calling getAllYearsFulfilledObligations" - {
      "must return the user's fulfilled obligations if found in the cache" in new SetUp {
        when(mockRepository.get(eqTo(appaId))) thenReturn Future.successful(Some(userFulfilledObligations))

        whenReady(fulfilledObligationsRepositoryService.getAllYearsFulfilledObligations(appaId, startYear, endYear)) {
          result =>
            result mustBe Right(fulfilledObligationsData)

            verify(mockRepository, times(1)).get(eqTo(appaId))
            verify(mockRepository, times(0)).set(any())
            verify(mockAlcoholDutyService, times(0)).getFulfilledObligations(any(), any())(any())
        }
      }

      "must return the user's fulfilled obligations obtained from the connector if not found in the cache" in new SetUp {
        when(mockRepository.get(eqTo(appaId))) thenReturn Future.successful(None)
        when(mockRepository.set(eqTo(userFulfilledObligations))) thenReturn Future.successful(userFulfilledObligations)

        when(mockAlcoholDutyService.getFulfilledObligations(eqTo(appaId), eqTo(2023))(any())) thenReturn EitherT
          .rightT(fulfilledObligations2023)
        when(mockAlcoholDutyService.getFulfilledObligations(eqTo(appaId), eqTo(2024))(any())) thenReturn EitherT
          .rightT(fulfilledObligations2024)
        when(mockAlcoholDutyService.getFulfilledObligations(eqTo(appaId), eqTo(2025))(any())) thenReturn EitherT
          .rightT(fulfilledObligations2025)

        whenReady(fulfilledObligationsRepositoryService.getAllYearsFulfilledObligations(appaId, startYear, endYear)) {
          result =>
            result mustBe Right(fulfilledObligationsData)

            verify(mockRepository, times(1)).get(eqTo(appaId))
            verify(mockRepository, times(1)).set(eqTo(userFulfilledObligations))
            verify(mockAlcoholDutyService, times(1)).getFulfilledObligations(eqTo(appaId), eqTo(2023))(any())
            verify(mockAlcoholDutyService, times(1)).getFulfilledObligations(eqTo(appaId), eqTo(2024))(any())
            verify(mockAlcoholDutyService, times(1)).getFulfilledObligations(eqTo(appaId), eqTo(2025))(any())
        }
      }

      "must return an ErrorResponse and not make subsequent calls if the connector returns an error" in new SetUp {
        when(mockRepository.get(eqTo(appaId))) thenReturn Future.successful(None)

        when(mockAlcoholDutyService.getFulfilledObligations(eqTo(appaId), eqTo(2023))(any())) thenReturn EitherT
          .rightT(fulfilledObligations2023)
        when(mockAlcoholDutyService.getFulfilledObligations(eqTo(appaId), eqTo(2024))(any())) thenReturn EitherT
          .leftT(ErrorCodes.unexpectedResponse)

        whenReady(fulfilledObligationsRepositoryService.getAllYearsFulfilledObligations(appaId, startYear, endYear)) {
          result =>
            result mustBe Left(ErrorCodes.unexpectedResponse)

            verify(mockRepository, times(1)).get(eqTo(appaId))
            verify(mockRepository, times(0)).set(any())
            verify(mockAlcoholDutyService, times(1)).getFulfilledObligations(eqTo(appaId), eqTo(2023))(any())
            verify(mockAlcoholDutyService, times(1)).getFulfilledObligations(eqTo(appaId), eqTo(2024))(any())
            verify(mockAlcoholDutyService, times(0)).getFulfilledObligations(eqTo(appaId), eqTo(2025))(any())
        }
      }
    }
  }

  class SetUp {
    val mockAlcoholDutyService: AlcoholDutyService         = mock[AlcoholDutyService]
    val mockRepository: UserFulfilledObligationsRepository = mock[UserFulfilledObligationsRepository]
    val fulfilledObligationsRepositoryService              =
      new FulfilledObligationsRepositoryService(mockAlcoholDutyService, mockRepository, clock)

    val startYear = 2023
    val endYear   = 2025
  }
}

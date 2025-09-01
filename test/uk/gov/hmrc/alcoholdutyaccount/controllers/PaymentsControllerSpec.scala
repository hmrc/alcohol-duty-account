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

package uk.gov.hmrc.alcoholdutyaccount.controllers

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.{*, eqTo}
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.payments.OpenPayments
import uk.gov.hmrc.alcoholdutyaccount.service.{HistoricPaymentsRepositoryService, OpenPaymentsService}
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.concurrent.Future

class PaymentsControllerSpec extends SpecBase {

  "PaymentsController" - {
    "when calling outstandingPayments" - {
      "return OK when the service returns success" in new SetUp {
        mockOpenPaymentsService.getOpenPayments(eqTo(appaId))(*) returnsF noOpenPayments

        val result: Future[Result] = controller.openPayments(appaId)(fakeRequest)
        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(noOpenPayments)
      }

      "return any error returned from the service" in new SetUp {
        when(mockOpenPaymentsService.getOpenPayments(eqTo(appaId))(*))
          .thenReturn(EitherT.leftT[Future, OpenPayments](ErrorCodes.unexpectedResponse))

        val result: Future[Result] = controller.openPayments(appaId)(fakeRequest)
        status(result)                          mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result).as[ErrorResponse] mustBe ErrorCodes.unexpectedResponse
      }
    }

    "when calling historicPayments" - {
      "return OK when the service returns success (minimum year is within the last 7 years)" in new SetUp {
        when(mockAppConfig.minimumHistoricPaymentsYear) thenReturn 2024
        when(
          mockHistoricPaymentsRepositoryService.getAllYearsHistoricPayments(eqTo(appaId), any(), any())(*)
        ) thenReturn Future.successful(Right(historicPaymentsData))

        val result: Future[Result] = controller.historicPayments(appaId)(fakeRequest)
        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(historicPaymentsData)

        verify(mockHistoricPaymentsRepositoryService, times(1))
          .getAllYearsHistoricPayments(eqTo(appaId), eqTo(2024), eqTo(2025))(any())
      }

      "return OK when the service returns success (minimum year is not within the last 7 years)" in new SetUp {
        when(mockAppConfig.minimumHistoricPaymentsYear) thenReturn 2015
        when(
          mockHistoricPaymentsRepositoryService.getAllYearsHistoricPayments(eqTo(appaId), any(), any())(*)
        ) thenReturn Future.successful(Right(historicPaymentsData))

        val result: Future[Result] = controller.historicPayments(appaId)(fakeRequest)
        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(historicPaymentsData)

        verify(mockHistoricPaymentsRepositoryService, times(1))
          .getAllYearsHistoricPayments(eqTo(appaId), eqTo(2019), eqTo(2025))(any())
      }

      "return any error returned from the service" in new SetUp {
        when(mockAppConfig.minimumHistoricPaymentsYear) thenReturn 2024
        when(
          mockHistoricPaymentsRepositoryService.getAllYearsHistoricPayments(eqTo(appaId), any(), any())(*)
        ).thenReturn(Future.successful(Left(ErrorCodes.unexpectedResponse)))

        val result: Future[Result] = controller.historicPayments(appaId)(fakeRequest)
        status(result)                          mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result).as[ErrorResponse] mustBe ErrorCodes.unexpectedResponse

        verify(mockHistoricPaymentsRepositoryService, times(1))
          .getAllYearsHistoricPayments(eqTo(appaId), eqTo(2024), eqTo(2025))(any())
      }
    }
  }

  class SetUp {
    val mockOpenPaymentsService               = mock[OpenPaymentsService]
    val mockHistoricPaymentsRepositoryService = mock[HistoricPaymentsRepositoryService]
    val mockAppConfig                         = mock[AppConfig]
    val cc                                    = Helpers.stubControllerComponents()

    val controller = new PaymentsController(
      fakeAuthorisedAction,
      fakeCheckAppaIdAction,
      mockOpenPaymentsService,
      mockHistoricPaymentsRepositoryService,
      mockAppConfig,
      cc,
      clock2025
    )
  }
}

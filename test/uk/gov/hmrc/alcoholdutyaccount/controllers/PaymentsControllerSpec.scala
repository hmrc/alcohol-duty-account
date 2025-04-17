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
import org.mockito.ArgumentMatchersSugar.{*, eqTo}
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.payments.OpenPayments
import uk.gov.hmrc.alcoholdutyaccount.service.{HistoricPaymentsService, OpenPaymentsService}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
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
      "return OK when the service returns success" in new SetUp {
        mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(year))(*) returnsF noHistoricPayments

        val result: Future[Result] = controller.historicPayments(appaId, year)(fakeRequest)
        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(noHistoricPayments)
      }

      "return any error returned from the service" in new SetUp {
        when(mockHistoricPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(year))(*))
          .thenReturn(EitherT.fromEither(Left(ErrorCodes.unexpectedResponse)))

        val result: Future[Result] = controller.historicPayments(appaId, year)(fakeRequest)
        status(result)                          mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result).as[ErrorResponse] mustBe ErrorCodes.unexpectedResponse
      }

      "return an error if the year is before the minimum" in new SetUp {
        val minimumYear = appConfig.minimumHistoricPaymentsYear

        val result: Future[Result] = controller.historicPayments(appaId, minimumYear - 1)(fakeRequest)
        status(result)                                  mustBe BAD_REQUEST
        contentAsJson(result).as[ErrorResponse].message mustBe "Bad request made"
      }

      "return an error if the year is after the current" in new SetUp {
        val result: Future[Result] = controller.historicPayments(appaId, LocalDate.now(clock).getYear + 1)(fakeRequest)
        status(result)                                  mustBe BAD_REQUEST
        contentAsJson(result).as[ErrorResponse].message mustBe "Bad request made"
      }
    }
  }

  class SetUp {
    val mockOpenPaymentsService: OpenPaymentsService         = mock[OpenPaymentsService]
    val mockHistoricPaymentsService: HistoricPaymentsService = mock[HistoricPaymentsService]
    val cc                                                   = Helpers.stubControllerComponents()

    val controller = new PaymentsController(
      fakeAuthorisedAction,
      fakeCheckAppaIdAction,
      mockOpenPaymentsService,
      mockHistoricPaymentsService,
      appConfig,
      cc,
      clock
    )

    val year: Int = 2024
  }
}

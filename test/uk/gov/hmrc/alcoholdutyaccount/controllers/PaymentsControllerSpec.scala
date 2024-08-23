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
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.service.PaymentsService
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class PaymentsControllerSpec extends SpecBase {

  "PaymentsController" - {
    "when calling outstandingPayments" - {
      "return OK when the service returns success" in new SetUp {
        mockPaymentsService.getOpenPayments(eqTo(appaId))(*) returnsF noOpenPayments

        val result: Future[Result] = controller.openPayments(appaId)(fakeRequest)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(noOpenPayments)
      }

      "return and sanitise any error returned from the service" in new SetUp {
        when(mockPaymentsService.getOpenPayments(eqTo(appaId))(*))
          .thenReturn(EitherT.fromEither(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))))

        val result: Future[Result] = controller.openPayments(appaId)(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result).as[ErrorResponse].message mustBe "Unexpected Response"
      }
    }

    "when calling historicPayments" - {
      "return OK when the service returns success" in new SetUp {
        mockPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(year))(*) returnsF noHistoricPayments

        val result: Future[Result] = controller.historicPayments(appaId, year)(fakeRequest)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(noHistoricPayments)
      }

      "return and sanitise any error returned from the service" in new SetUp {
        when(mockPaymentsService.getHistoricPayments(eqTo(appaId), eqTo(year))(*))
          .thenReturn(EitherT.fromEither(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))))

        val result: Future[Result] = controller.historicPayments(appaId, year)(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result).as[ErrorResponse].message mustBe "Unexpected Response"
      }
    }
  }

  class SetUp extends TestData {
    val mockPaymentsService: PaymentsService = mock[PaymentsService]
    val cc                                   = Helpers.stubControllerComponents()
    val controller                           = new PaymentsController(fakeAuthorisedAction, mockPaymentsService, cc)

    val year: Int = 2024
  }
}

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
import org.mockito.MockitoSugar.{mock, never, times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.{FakeRequest, Helpers}
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.alcoholdutyaccount.models.AlcoholDutyCardData
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ObligationDetails, Open}
import uk.gov.hmrc.alcoholdutyaccount.service.AlcoholDutyService
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import scala.concurrent.Future

class AlcoholDutyControllerSpec extends AnyWordSpec with Matchers {

  "GET /obligationDetails" should {
    "return OK when is called with a valid alcoholDutyReference" in new SetUp {
      alcoholDutyService.getObligations(eqTo(alcoholDutyReference), eqTo(returnPeriod))(*) returnsF obligationDetails

      val result: Future[Result] = controller.obligationDetails(alcoholDutyReference, periodKey)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(obligationDetails)
    }

    "return BAD_REQUEST when periodKey is invalid" in new SetUp {
      alcoholDutyService.getObligations(*, *)(*) returnsF obligationDetails

      val result: Future[Result] = controller.obligationDetails(alcoholDutyReference, badPeriodKey)(FakeRequest())
      status(result) mustBe BAD_REQUEST

      verify(alcoholDutyService, never).getObligations(*, *)(*)
    }

    "return any error returned from the service" in new SetUp {
      when(alcoholDutyService.getObligations(*, *)(*))
        .thenReturn(EitherT.fromEither(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))))

      val result: Future[Result] = controller.obligationDetails(alcoholDutyReference, periodKey)(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR

      verify(alcoholDutyService, times(1)).getObligations(*, *)(*)
    }
  }

  "GET /bta-tile-data" should {
    "return 200 when is called with a valid alcoholDutyReference" in new SetUp {
      alcoholDutyService.getAlcoholDutyCardData(*)(*) returnsF cardData

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(cardData)
    }

    "return 400 when is called with an invalid alcoholDutyReference" in new SetUp {
      val expectedError = ErrorResponse(BAD_REQUEST, "Invalid alcohol duty reference")

      when(alcoholDutyService.getAlcoholDutyCardData(*)(*)).thenReturn(EitherT.fromEither(Left(expectedError)))

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(expectedError)
    }
  }

  class SetUp {
    val alcoholDutyService: AlcoholDutyService = mock[AlcoholDutyService]
    val cc                                     = Helpers.stubControllerComponents()
    val controller                             = new AlcoholDutyController(alcoholDutyService, cc)

    val alcoholDutyReference = "XMADP0000000200"
    val periodKey            = "24AE"
    val badPeriodKey         = "blah"
    val returnPeriod         = ReturnPeriod(periodKey, 2024, 5)
    val periodStart          = LocalDate.of(2023, 1, 1)
    val periodEnd            = LocalDate.of(2023, 1, 31)
    val obligationDetails    = ObligationDetails(
      status = Open,
      inboundCorrespondenceFromDate = periodStart,
      inboundCorrespondenceToDate = periodEnd,
      inboundCorrespondenceDateReceived = None,
      inboundCorrespondenceDueDate = LocalDate.now().plusDays(1),
      periodKey = periodKey
    )

    val cardData = AlcoholDutyCardData(
      alcoholDutyReference = "testAlcoholDutyReference",
      approvalStatus = Approved,
      hasReturnsError = false,
      hasPaymentError = false,
      returns = Returns(dueReturnExists = Some(true), numberOfOverdueReturns = Some(0)),
      payments =
        Payments(balance = Some(Balance(isMultiplePaymentDue = true, totalPaymentAmount = 2, chargeReference = None)))
    )
  }
}

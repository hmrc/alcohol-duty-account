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
import play.api.test.Helpers
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.AlcoholDutyCardData
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.ApprovalStatus.Approved
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.service.AlcoholDutyService
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.concurrent.Future

class AlcoholDutyControllerSpec extends SpecBase {

  "GET /subscriptionSummary" - {
    "return OK when is called with a valid appaId" in new SetUp {
      alcoholDutyService.getSubscriptionSummary(eqTo(appaId))(*) returnsF approvedAdrSubscriptionSummary

      val result: Future[Result] = controller.subscriptionSummary(appaId)(fakeRequest)
      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(approvedAdrSubscriptionSummary)
    }

    "return any error returned from the service" in new SetUp {
      when(alcoholDutyService.getSubscriptionSummary(*)(*))
        .thenReturn(EitherT.fromEither(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))))

      val result: Future[Result] = controller.subscriptionSummary(appaId)(fakeRequest)
      status(result) mustBe INTERNAL_SERVER_ERROR

      verify(alcoholDutyService, times(1)).getSubscriptionSummary(*)(*)
    }
  }

  "GET /openObligationDetails" - {
    "return OK when is called with a valid appaId" in new SetUp {
      alcoholDutyService.getOpenObligations(eqTo(appaId), eqTo(periodKey))(
        *
      ) returnsF adrObligationDetails

      val result: Future[Result] = controller.openObligationDetails(appaId, periodKey)(fakeRequest)
      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(adrObligationDetails)
    }

    "return BAD_REQUEST when periodKey is invalid" in new SetUp {
      alcoholDutyService.getOpenObligations(*, *)(*) returnsF adrObligationDetails

      val result: Future[Result] = controller.openObligationDetails(appaId, badPeriodKey)(fakeRequest)
      status(result) mustBe BAD_REQUEST

      verify(alcoholDutyService, never).getOpenObligations(*, *)(*)
    }

    "return any error returned from the service" in new SetUp {
      when(alcoholDutyService.getOpenObligations(*, *)(*))
        .thenReturn(EitherT.fromEither(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))))

      val result: Future[Result] = controller.openObligationDetails(appaId, periodKey)(fakeRequest)
      status(result) mustBe INTERNAL_SERVER_ERROR

      verify(alcoholDutyService, times(1)).getOpenObligations(*, *)(*)
    }
  }
  "GET /obligationDetails" - {
    "return OK when is called with a valid appaId" in new SetUp {
      alcoholDutyService.getObligations(eqTo(appaId), None)(
        *
      ) returnsF adrMultipleOpenAndFulfilledData

      val result: Future[Result] = controller.obligationDetails(appaId)(fakeRequest)
      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(adrMultipleOpenAndFulfilledData)
    }

    "return any error returned from the service" in new SetUp {
      when(alcoholDutyService.getObligations(*, *)(*))
        .thenReturn(EitherT.fromEither(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))))

      val result: Future[Result] = controller.obligationDetails(appaId)(fakeRequest)
      status(result) mustBe INTERNAL_SERVER_ERROR

      verify(alcoholDutyService, times(1)).getObligations(*, *)(*)
    }
  }

  "GET /bta-tile-data" - {
    "return 200 when is called with a valid appaId and available" in new SetUp {
      when(appConfig.btaServiceAvailable).thenReturn(true)
      alcoholDutyService.getAlcoholDutyCardData(*)(*) returnsF cardData

      val result: Future[Result] = controller.btaTileData(appaId)(fakeRequest)
      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(cardData)
    }

    "return 400 when is called with an invalid appaId" in new SetUp {
      when(appConfig.btaServiceAvailable).thenReturn(true)

      val expectedError = ErrorResponse(BAD_REQUEST, "Invalid alcohol duty reference")

      when(alcoholDutyService.getAlcoholDutyCardData(*)(*)).thenReturn(EitherT.fromEither(Left(expectedError)))

      val result: Future[Result] = controller.btaTileData(appaId)(fakeRequest)
      status(result)        mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(expectedError)
    }

    "return 503 when shuttered" in new SetUp {
      when(appConfig.btaServiceAvailable).thenReturn(false)

      alcoholDutyService.getAlcoholDutyCardData(*)(*) returnsF cardData

      val result: Future[Result] = controller.btaTileData(appaId)(fakeRequest)
      status(result)        mustBe SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe Json.toJson(ErrorCodes.serviceUnavailable)
    }
  }

  class SetUp {
    val appConfig                              = mock[AppConfig]
    val alcoholDutyService: AlcoholDutyService = mock[AlcoholDutyService]
    val cc                                     = Helpers.stubControllerComponents()
    val controller                             =
      new AlcoholDutyController(fakeAuthorisedAction, fakeCheckAppaIdAction, alcoholDutyService, appConfig, cc)

    val badPeriodKey = "blah"

    val cardData = AlcoholDutyCardData(
      alcoholDutyReference = appaId,
      approvalStatus = Some(Approved),
      hasSubscriptionSummaryError = false,
      hasReturnsError = false,
      hasPaymentsError = false,
      returns = Returns(dueReturnExists = Some(true), numberOfOverdueReturns = Some(0)),
      payments =
        Payments(balance = Some(Balance(isMultiplePaymentDue = true, totalPaymentAmount = 2, chargeReference = None))),
      contactPreference = Some("digital"),
      emailBounced = Some(false)
    )
  }
}

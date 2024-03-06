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
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.MockitoSugar.mock
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.{FakeRequest, Helpers}
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.alcoholdutyaccount.models.AlcoholDutyCardData
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.service.AlcoholDutyService

import scala.concurrent.Future

class AlcoholDutyControllerSpec extends AnyWordSpec with Matchers {

  val alcoholDutyService: AlcoholDutyService = mock[AlcoholDutyService]
  val cc                                     = Helpers.stubControllerComponents()
  val controller                             = new AlcoholDutyController(alcoholDutyService, cc)

  "GET /bta-tile-data" should {
    "return 200 when is called with a valid alcoholDutyReference" in {
      val alcoholDutyReference = "testAlcoholDutyReference"
      val expectedData         = AlcoholDutyCardData(
        alcoholDutyReference = "testAlcoholDutyReference",
        approvalStatus = Approved,
        hasReturnsError = false,
        hasPaymentError = false,
        returns = Returns(dueReturnExists = Some(true), numberOfOverdueReturns = Some(0)),
        payments = Payments(isMultiplePaymentDue = Some(true), totalPaymentAmount = Some(2), chargeReference = None)
      )

      alcoholDutyService.getAlcoholDutyCardData(*)(*) returnsF expectedData

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(expectedData)
    }

    "return 400 when is called with an invalid alcoholDutyReference" in {
      val alcoholDutyReference = "testAlcoholDutyReference"
      val expectedError        = ErrorResponse(BAD_REQUEST, "Invalid alcohol duty reference")

      alcoholDutyService.getAlcoholDutyCardData(*)(*) returns EitherT.leftT(expectedError)

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(expectedError)
    }
  }
}

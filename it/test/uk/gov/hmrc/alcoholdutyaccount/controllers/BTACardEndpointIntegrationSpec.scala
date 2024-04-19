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

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.alcoholdutyaccount.base.ISpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.{AlcoholDutyCardData, Approved, Balance, InsolventCardData, Payments, Returns}

class BTACardEndpointIntegrationSpec
  extends ISpecBase {

  "the service BTA Card endpoint should" should {
    "respond with 200 status" in {
      stubAuthorised()
      stubGetSubscriptionSummary(approvedSubscriptionSummary)
      stubGetObligations(obligationDataSingleOpen)
      stubGetFinancialData(financialDocument)

      val expectedBTATileData = AlcoholDutyCardData(
        alcoholDutyReference = alcoholDutyReference,
        approvalStatus = Approved,
        hasReturnsError = false,
        hasPaymentError = false,
        returns = Returns(
          dueReturnExists=Some(false),
          numberOfOverdueReturns=Some(1)
        ),
        payments = Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = 100,
              isMultiplePaymentDue = false,
              chargeReference = Some("X1234567890")
            )
          )
        )
      )


      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
    }

    "respond with 200 status for insolvent subscription" in {
      stubAuthorised()
      stubGetSubscriptionSummary(insolventSubscriptionSummary)
      stubGetObligations(obligationDataSingleOpen)
      stubGetFinancialData(financialDocument)

      val expectedBTACardData = InsolventCardData(alcoholDutyReference)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(expectedBTACardData)
    }

    "respond with NOT_IMPLEMENTED status for subscription status de-registered" in {
      stubAuthorised()
      stubGetSubscriptionSummary(deregisteredSubscriptionSummary)
      stubGetObligations(obligationDataSingleOpen)
      stubGetFinancialData(financialDocument)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_IMPLEMENTED
    }

    "respond with NOT_IMPLEMENTED status for subscription status revoked" in {
      stubAuthorised()
      stubGetSubscriptionSummary(revokedSubscriptionSummary)
      stubGetObligations(obligationDataSingleOpen)
      stubGetFinancialData(financialDocument)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_IMPLEMENTED
    }

    "respond with NOT_FOUND status for errors in the subscription api call" in {
      stubAuthorised()
      stubSubscriptionSummaryNotFound()
      stubGetObligations(obligationDataSingleOpen)
      stubGetFinancialData(financialDocument)

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
    }

    "respond with 200 status for errors in the obligation api call" in {
      stubAuthorised()
      stubGetSubscriptionSummary(approvedSubscriptionSummary)
      stubObligationsNotFound()
      stubGetFinancialData(financialDocument)

      val expectedBTATileData = AlcoholDutyCardData(
        alcoholDutyReference = alcoholDutyReference,
        approvalStatus = Approved,
        hasReturnsError = true,
        hasPaymentError = false,
        returns = Returns(),
        payments = Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = 100,
              isMultiplePaymentDue = false,
              chargeReference = Some("X1234567890")
            )
          )
        )
      )

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
    }

    "respond with 200 status for errors in the financial api call" in {
      stubAuthorised()
      stubGetSubscriptionSummary(approvedSubscriptionSummary)
      stubGetObligations(obligationDataSingleOpen)
      stubFinancialDataNotFound()

      val expectedBTATileData = AlcoholDutyCardData(
        alcoholDutyReference = alcoholDutyReference,
        approvalStatus = Approved,
        hasReturnsError = false,
        hasPaymentError = true,
        returns = Returns(
          dueReturnExists=Some(false),
          numberOfOverdueReturns=Some(1)
        ),
        payments = Payments()
      )

      val response = callRoute(
        FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
    }
  }
}

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
import uk.gov.hmrc.alcoholdutyaccount.base.{FinancialDataStubs, ISpecBase, ObligationDataStubs, SubscriptionSummaryStubs}
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus.{Approved, DeRegistered, Insolvent, Revoked, SmallCiderProducer}
import uk.gov.hmrc.alcoholdutyaccount.models.{AlcoholDutyCardData, Balance, Payments, RestrictedCardData, Returns}

class BTACardEndpointIntegrationSpec
    extends ISpecBase
    with FinancialDataStubs
    with ObligationDataStubs
    with SubscriptionSummaryStubs {

  "the service BTA Card endpoint should" should {

    val alcoholDutyReference: String = generateAlcoholDutyReference().sample.get

    "respond with 200 status and with full card data" when {
      "the status is approved" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, approvedSubscriptionSummary)
        stubGetObligations(alcoholDutyReference, obligationDataSingleOpen)
        stubGetFinancialData(alcoholDutyReference, financialDocument)

        val expectedBTATileData = AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = Some(Approved),
          hasSubscriptionSummaryError = false,
          hasReturnsError = false,
          hasPaymentError = false,
          returns = Returns(
            dueReturnExists = Some(false),
            numberOfOverdueReturns = Some(1),
            periodKey = Some("24AE")
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

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
      }

      "the insolvent flag is on" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, insolventSubscriptionSummary)
        stubGetObligations(alcoholDutyReference, obligationDataSingleOpen)
        stubGetFinancialData(alcoholDutyReference, financialDocument)

        val expectedBTATileData = AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = Some(Insolvent),
          hasSubscriptionSummaryError = false,
          hasReturnsError = false,
          hasPaymentError = false,
          returns = Returns(
            dueReturnExists = Some(false),
            numberOfOverdueReturns = Some(1),
            periodKey = Some("24AE")
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

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
      }
    }

    "respond with 200 status and with Restricted Card Data" when {
      "subscription has DeRegistered status" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, deregisteredSubscriptionSummary)

        val expectedBTACardData = RestrictedCardData(alcoholDutyReference, DeRegistered)

        val response = callRoute(
          FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
            .withHeaders("Authorization" -> "Bearer 12345")
        )

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTACardData)
      }

      "subscription has Revoked status" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, revokedSubscriptionSummary)

        val expectedBTACardData = RestrictedCardData(alcoholDutyReference, Revoked)

        val response = callRoute(
          FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
            .withHeaders("Authorization" -> "Bearer 12345")
        )

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTACardData)
      }

      "subscription has small producer flag on" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, smallCiderProducerSubscriptionSummary)

        val expectedBTACardData = RestrictedCardData(alcoholDutyReference, SmallCiderProducer)

        val response = callRoute(
          FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
            .withHeaders("Authorization" -> "Bearer 12345")
        )

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTACardData)
      }
    }

    "respond with 200 status for errors" when {
      "subscription summary api call fails" in {
        stubAuthorised()
        stubSubscriptionSummaryError(alcoholDutyReference)

        val expectedBTATileData = AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = None,
          hasSubscriptionSummaryError = true,
          hasReturnsError = false,
          hasPaymentError = false,
          returns = Returns(),
          payments = Payments()
        )

        val response = callRoute(
          FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
            .withHeaders("Authorization" -> "Bearer 12345")
        )

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
      }

      "obligation api call fails" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, approvedSubscriptionSummary)
        stubObligationsNotFound(alcoholDutyReference)
        stubGetFinancialData(alcoholDutyReference, financialDocument)

        val expectedBTATileData = AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = Some(Approved),
          hasSubscriptionSummaryError = false,
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

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
      }

      "financial data api call fails" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, approvedSubscriptionSummary)
        stubGetObligations(alcoholDutyReference, obligationDataSingleOpen)
        stubFinancialDataNotFound(alcoholDutyReference)

        val expectedBTATileData = AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = Some(Approved),
          hasSubscriptionSummaryError = false,
          hasReturnsError = false,
          hasPaymentError = true,
          returns = Returns(
            dueReturnExists = Some(false),
            numberOfOverdueReturns = Some(1),
            periodKey = Some("24AE")
          ),
          payments = Payments()
        )

        val response = callRoute(
          FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
            .withHeaders("Authorization" -> "Bearer 12345")
        )

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
      }

      "both obligation api and financial data api calls fail" in {
        stubAuthorised()
        stubGetSubscriptionSummary(alcoholDutyReference, approvedSubscriptionSummary)
        stubObligationsNotFound(alcoholDutyReference)
        stubFinancialDataNotFound(alcoholDutyReference)

        val expectedBTATileData = AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = Some(Approved),
          hasSubscriptionSummaryError = false,
          hasReturnsError = true,
          hasPaymentError = true,
          returns = Returns(),
          payments = Payments()
        )

        val response = callRoute(
          FakeRequest("GET", routes.AlcoholDutyController.btaTileData(alcoholDutyReference).url)
            .withHeaders("Authorization" -> "Bearer 12345")
        )

        status(response)        shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(expectedBTATileData)
      }
    }
  }
}

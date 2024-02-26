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

import cats.data.OptionT
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.MockitoSugar.{mock, when}
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
import uk.gov.hmrc.alcoholdutyaccount.connectors.{ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models.AlcoholDutyCardData
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Beer, CiderOrPerry, Obligation, ObligationData, ObligationDetails, Open, Spirits, SubscriptionSummary, WineAndOtherFermentedProduct}

import java.time.LocalDate
import scala.concurrent.Future

class AlcoholDutyControllerSpec extends AnyWordSpec with Matchers {
  val subscriptionSummaryConnector: SubscriptionSummaryConnector = mock[SubscriptionSummaryConnector]
  val obligationDataConnector: ObligationDataConnector           = mock[ObligationDataConnector]
  val cc                                                         = Helpers.stubControllerComponents()

  val controller = new AlcoholDutyController(subscriptionSummaryConnector, obligationDataConnector, cc)

  val alcoholDutyReference = "testAlcoholDutyReference"

  val obligationDataOneDue = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(
          ObligationDetails(
            status = Open,
            inboundCorrespondenceFromDate = LocalDate.parse("2024-01-01"),
            inboundCorrespondenceToDate = LocalDate.parse("2024-01-31"),
            inboundCorrespondenceDueDate = LocalDate.now().plusDays(10),
            inboundCorrespondenceDateReceived = None,
            periodKey = "24XY"
          )
        )
      )
    )
  )

  val obligationDataOneDueOneOverdue = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(
          ObligationDetails(
            status = Open,
            inboundCorrespondenceFromDate = LocalDate.parse("2024-01-01"),
            inboundCorrespondenceToDate = LocalDate.parse("2024-01-31"),
            inboundCorrespondenceDueDate = LocalDate.now().plusDays(10),
            inboundCorrespondenceDateReceived = None,
            periodKey = "24XY"
          ),
          ObligationDetails(
            status = Open,
            inboundCorrespondenceFromDate = LocalDate.parse("2024-01-01"),
            inboundCorrespondenceToDate = LocalDate.parse("2024-01-31"),
            inboundCorrespondenceDueDate = LocalDate.now().minusDays(10),
            inboundCorrespondenceDateReceived = None,
            periodKey = "24XY"
          )
        )
      )
    )
  )

  val subscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
    smallCiderFlag = false,
    approvalStatus = hods.Approved,
    insolvencyFlag = false
  )

  val subscriptionSummaryInsolvent = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set.empty,
    smallCiderFlag = false,
    approvalStatus = hods.Approved,
    insolvencyFlag = true
  )

  val subscriptionSummaryRevoked = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set.empty,
    smallCiderFlag = false,
    approvalStatus = hods.Revoked,
    insolvencyFlag = false
  )

  val subscriptionSummaryDeRegistered = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set.empty,
    smallCiderFlag = false,
    approvalStatus = hods.DeRegistered,
    insolvencyFlag = false
  )

  "GET /bta-tile-data" should {
    "return 200 when is called with a valid alcoholDutyReference" in {

      val expectedData = AlcoholDutyCardData(
        alcoholDutyReference = "testAlcoholDutyReference",
        approvalStatus = Approved,
        hasReturnsError = false,
        returns = Return(dueReturnExists = Some(true), numberOfOverdueReturns = Some(0))
      )

      subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference)(*) returnsF subscriptionSummary
      obligationDataConnector.getObligationData(alcoholDutyReference)(*) returnsF obligationDataOneDue

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(expectedData)
    }

    "return 200 when is called with a valid alcoholDutyReference and the correct amount of overdue returns in the Return section" in {

      val expectedData = AlcoholDutyCardData(
        alcoholDutyReference = "testAlcoholDutyReference",
        approvalStatus = Approved,
        hasReturnsError = false,
        returns = Return(dueReturnExists = Some(true), numberOfOverdueReturns = Some(1))
      )

      subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference)(*) returnsF subscriptionSummary
      obligationDataConnector.getObligationData(alcoholDutyReference)(*) returnsF obligationDataOneDueOneOverdue

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(expectedData)
    }

    "return 200 when is called with a valid alcoholDutyReference with an error in the return" in {

      val expectedData = AlcoholDutyCardData(
        alcoholDutyReference = "testAlcoholDutyReference",
        approvalStatus = Approved,
        hasReturnsError = true,
        returns = Return()
      )

      subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference)(*) returnsF subscriptionSummary
      when(obligationDataConnector.getObligationData(*)(*))
        .thenReturn(OptionT.none[Future, ObligationData])

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(expectedData)
    }

    "return 200 when is called with a valid alcoholDutyReference but not call the obligation data api if the status is Insolvent" in {
      val expectedData = AlcoholDutyCardData(
        alcoholDutyReference = "testAlcoholDutyReference",
        approvalStatus = Insolvent,
        hasReturnsError = false,
        returns = Return()
      )

      subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference)(*) returnsF subscriptionSummaryInsolvent

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(expectedData)
    }

    "return 400 when is called with an invalid alcoholDutyReference" in {

      when(subscriptionSummaryConnector.getSubscriptionSummary(*)(*))
        .thenReturn(OptionT.none[Future, SubscriptionSummary])

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe BAD_REQUEST
    }

    "return 400 when is called with a valid alcoholDutyReference but the status Revoked" in {

      subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference)(*) returnsF subscriptionSummaryRevoked

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe BAD_REQUEST
    }

    "return 400 when is called with a valid alcoholDutyReference but the status De-registered" in {

      subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference)(
        *
      ) returnsF subscriptionSummaryDeRegistered

      val result: Future[Result] = controller.btaTileData(alcoholDutyReference)(FakeRequest())
      status(result) mustBe BAD_REQUEST
    }
  }
}

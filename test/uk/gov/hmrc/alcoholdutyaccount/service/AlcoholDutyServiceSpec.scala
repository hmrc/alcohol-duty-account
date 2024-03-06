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

import cats.data.OptionT
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.matchers.should.Matchers
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{NOT_FOUND, NOT_IMPLEMENTED}
import uk.gov.hmrc.alcoholdutyaccount.connectors.{FinancialDataConnector, ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Beer, Document, FinancialTransaction, FinancialTransactionItem, Fulfilled, Obligation, ObligationData, ObligationDetails, Open, SubscriptionSummary}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class AlcoholDutyServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val subscriptionSummaryConnector = mock[SubscriptionSummaryConnector]
  val obligationDataConnector      = mock[ObligationDataConnector]
  val financialDataConnector       = mock[FinancialDataConnector]
  val service                      = new AlcoholDutyService(subscriptionSummaryConnector, obligationDataConnector, financialDataConnector)

  val periodStart = LocalDate.of(2023, 1, 1)
  val periodEnd   = LocalDate.of(2023, 1, 31)

  "AlcoholDutyService" should {
    "extract and empty Returns object when there are no obligation details" in {
      val obligationDetails = Seq.empty
      val result            = service.extractReturns(obligationDetails)
      result shouldBe Returns()
    }

    "extract a Returns object with a dueReturnExists true if the due date is in a week" in {
      val obligationDetails = Seq(
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.now().plusDays(7),
          periodKey = "11AA"
        )
      )
      val result            = service.extractReturns(obligationDetails)
      result shouldBe Returns(
        dueReturnExists = Some(true),
        numberOfOverdueReturns = Some(0)
      )
    }

    "extract a Returns object with dueReturnExists true if the due date is today" in {
      val obligationDetails = Seq(
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.now(),
          periodKey = "11AA"
        )
      )
      val result            = service.extractReturns(obligationDetails)
      result shouldBe Returns(
        dueReturnExists = Some(true),
        numberOfOverdueReturns = Some(0)
      )
    }

    "extract a Returns object with dueReturnExists false and the number of overdue returns equals 1 if the due date was yesterday" in {
      val obligationDetails = Seq(
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.now().minusDays(1),
          periodKey = "11AA"
        )
      )
      val result            = service.extractReturns(obligationDetails)
      result shouldBe Returns(
        dueReturnExists = Some(false),
        numberOfOverdueReturns = Some(1)
      )
    }

    "extract a Returns object with dueReturnExists true and the correct number of overdue returns ignoring fulfilled obligations" in {
      val obligationDetails = Seq(
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.now().plusDays(1),
          periodKey = "11AA"
        ),
        ObligationDetails(
          status = Fulfilled,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = Some(LocalDate.now().minusDays(1).minusMonths(1)),
          inboundCorrespondenceDueDate = LocalDate.now().minusDays(1),
          periodKey = "12AA"
        ),
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.now().minusDays(1),
          periodKey = "13AA"
        ),
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = periodStart,
          inboundCorrespondenceToDate = periodEnd,
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.now().minusDays(2),
          periodKey = "14AA"
        )
      )
      val result            = service.extractReturns(obligationDetails)
      result shouldBe Returns(
        dueReturnExists = Some(true),
        numberOfOverdueReturns = Some(2)
      )
    }

    "extract an empty Payments object if there are no financial transactions" in {
      val financialDocument = Document(
        financialTransactions = Seq.empty
      )
      val result            = service.extractPayments(financialDocument)
      result shouldBe Payments()
    }

    "extract a Payments object with a charging reference for a single financial transaction" in {
      val financialDocument = Document(
        financialTransactions = Seq(
          FinancialTransaction(
            periodKey = "18AA",
            chargeReference = "X1234567890",
            originalAmount = 1000.00,
            outstandingAmount = 50.00,
            mainTransaction = "1001",
            subTransaction = "1111",
            items = Seq(
              FinancialTransactionItem(
                subItem = "001",
                paymentAmount = 50.00
              )
            )
          ),
          FinancialTransaction(
            periodKey = "18AA",
            chargeReference = "X1234567890",
            originalAmount = 1000.00,
            outstandingAmount = 50.00,
            mainTransaction = "1001",
            subTransaction = "2222",
            items = Seq(
              FinancialTransactionItem(
                subItem = "002",
                paymentAmount = 100.00
              )
            )
          )
        )
      )
      val result            = service.extractPayments(financialDocument)
      result shouldBe Payments(
        totalPaymentAmount = Some(100.00),
        isMultiplePaymentDue = Some(false),
        chargeReference = Some("X1234567890")
      )
    }

    "extract a Payments object without a charging reference for multiple financial transactions" in {
      val financialDocument = Document(
        financialTransactions = Seq(
          FinancialTransaction(
            periodKey = "18AA",
            chargeReference = "XM002610011594",
            originalAmount = 1000.00,
            outstandingAmount = 50.00,
            mainTransaction = "1001",
            subTransaction = "1111",
            items = Seq(
              FinancialTransactionItem(
                subItem = "001",
                paymentAmount = 50.00
              )
            )
          ),
          FinancialTransaction(
            periodKey = "18AB",
            chargeReference = "XM002610011595",
            originalAmount = 2000.00,
            outstandingAmount = 100.00,
            mainTransaction = "1001",
            subTransaction = "1112",
            items = Seq(
              FinancialTransactionItem(
                subItem = "002",
                paymentAmount = 50.00
              )
            )
          ),
          FinancialTransaction(
            periodKey = "18AA",
            chargeReference = "XM002610011594",
            originalAmount = 3000.00,
            outstandingAmount = 200.00,
            mainTransaction = "1001",
            subTransaction = "1112",
            items = Seq(
              FinancialTransactionItem(
                subItem = "003",
                paymentAmount = 50.00
              )
            )
          )
        )
      )
      val result            = service.extractPayments(financialDocument)
      result shouldBe Payments(
        totalPaymentAmount = Some(350.00),
        isMultiplePaymentDue = Some(true),
        chargeReference = None
      )
    }
  }

  "getObligationData should return None if the obligationDataConnector returns None" in {
    when(obligationDataConnector.getObligationData(*)(*)).thenReturn(OptionT.none[Future, ObligationData])

    val result = service.getObligationData("testAlcoholDutyReference")
    result onComplete {
      case Success(value) => value shouldBe None
      case _              => fail("Expected a successful future")
    }
  }

  "getObligationData should return a Returns object if the obligationDataConnector returns an obligation" in {
    val obligationDataOneDue = ObligationData(obligations = Seq.empty)
    obligationDataConnector.getObligationData(*)(*) returnsF obligationDataOneDue

    service.getObligationData("testAlcoholDutyReference").onComplete { result =>
      result shouldBe Success(Some(Returns()))
    }
  }

  "getPaymentInformation should return None if the financialDataConnector returns None" in {
    when(financialDataConnector.getFinancialData(*)(*)).thenReturn(OptionT.none[Future, Document])

    val result = service.getPaymentInformation("testAlcoholDutyReference")
    result onComplete {
      case Success(value) => value shouldBe None
      case _              => fail("Expected a successful future")
    }
  }

  "getPaymentInformation should return a Payments object if the financialDataConnector returns a Document" in {
    val financialDocument = Document(financialTransactions = Seq.empty)
    financialDataConnector.getFinancialData(*)(*) returnsF financialDocument

    service.getPaymentInformation("testAlcoholDutyReference").onComplete { result =>
      result shouldBe Success(Some(Payments()))
    }
  }

  "getAlcoholDutyCardData should return a Returns and Payments object if the obligationDataConnector and financialDataConnector return data" in {
    val subscriptionSummary = SubscriptionSummary(
      typeOfAlcoholApprovedForList = Set(Beer),
      smallCiderFlag = false,
      approvalStatus = hods.Approved,
      insolvencyFlag = false
    )
    subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

    val obligationDataOneDue = ObligationData(obligations =
      Seq(
        Obligation(
          obligationDetails = Seq(
            ObligationDetails(
              status = Open,
              inboundCorrespondenceFromDate = periodStart,
              inboundCorrespondenceToDate = periodEnd,
              inboundCorrespondenceDateReceived = None,
              inboundCorrespondenceDueDate = LocalDate.now().plusDays(1),
              periodKey = "11AA"
            )
          )
        )
      )
    )
    obligationDataConnector.getObligationData(*)(*) returnsF obligationDataOneDue

    val financialDocument = Document(financialTransactions = Seq.empty)
    financialDataConnector.getFinancialData(*)(*) returnsF financialDocument

    whenReady(service.getAlcoholDutyCardData("testAlcoholDutyReference").value) { result =>
      result shouldBe Right(
        AlcoholDutyCardData(
          "testAlcoholDutyReference",
          Approved,
          false,
          false,
          Returns(Some(true), Some(0)),
          Payments()
        )
      )
    }
  }

  "getAlcoholDutyCardData should return a Returns empty object if the obligationDataConnector return an error" in {
    val subscriptionSummary = SubscriptionSummary(
      typeOfAlcoholApprovedForList = Set(Beer),
      smallCiderFlag = false,
      approvalStatus = hods.Approved,
      insolvencyFlag = false
    )
    subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

    obligationDataConnector.getObligationData(*)(*) returns OptionT.none[Future, ObligationData]

    val financialDocument = Document(financialTransactions = Seq.empty)
    financialDataConnector.getFinancialData(*)(*) returnsF financialDocument

    whenReady(service.getAlcoholDutyCardData("testAlcoholDutyReference").value) { result =>
      result shouldBe Right(
        AlcoholDutyCardData("testAlcoholDutyReference", Approved, true, false, Returns(), Payments())
      )
    }
  }

  "getAlcoholDutyCardData should return a Payments empty object if the financialDataConnector return an error" in {
    val subscriptionSummary = SubscriptionSummary(
      typeOfAlcoholApprovedForList = Set(Beer),
      smallCiderFlag = false,
      approvalStatus = hods.Approved,
      insolvencyFlag = false
    )
    subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

    val obligationDataOneDue = ObligationData(obligations = Seq.empty)
    obligationDataConnector.getObligationData(*)(*) returnsF obligationDataOneDue

    financialDataConnector.getFinancialData(*)(*) returns OptionT.none[Future, Document]

    whenReady(service.getAlcoholDutyCardData("testAlcoholDutyReference").value) { result =>
      result shouldBe Right(
        AlcoholDutyCardData("testAlcoholDutyReference", Approved, false, true, Returns(), Payments())
      )
    }
  }

  "getAlcoholDutyCardData should return an Insolvent Card Data if the subscription summary has insolvencyFlag set to true" in {
    val subscriptionSummary = SubscriptionSummary(
      typeOfAlcoholApprovedForList = Set(Beer),
      smallCiderFlag = false,
      approvalStatus = hods.Approved,
      insolvencyFlag = true
    )
    subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

    whenReady(service.getAlcoholDutyCardData("testAlcoholDutyReference").value) { result =>
      result shouldBe Right(
        InsolventCardData("testAlcoholDutyReference")
      )
    }
  }

  "getAlcoholDutyCardData should return an error if the subscription summary return an error" in {

    subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returns OptionT.none[Future, SubscriptionSummary]

    whenReady(service.getAlcoholDutyCardData("testAlcoholDutyReference").value) { result =>
      result shouldBe Left(
        ErrorResponse(NOT_FOUND, "Subscription Summary not found")
      )
    }
  }

  Seq(hods.Revoked, hods.DeRegistered).foreach { approvalStatus =>
    s"getAlcoholDutyCardData should return a NOT_IMPLEMENTED error if the subscription summary return a $approvalStatus type" in {
      val subscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = Set(Beer),
        smallCiderFlag = false,
        approvalStatus = approvalStatus,
        insolvencyFlag = false
      )
      subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

      whenReady(service.getAlcoholDutyCardData("testAlcoholDutyReference").value) { result =>
        result shouldBe Left(
          ErrorResponse(NOT_IMPLEMENTED, "Approval Status not yet supported")
        )
      }
    }
  }
}

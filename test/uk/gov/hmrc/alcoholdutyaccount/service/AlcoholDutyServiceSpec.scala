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

import cats.data.{EitherT, OptionT}
import org.mockito.MockitoSugar.{mock, when}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.cats.IdiomaticMockitoCats.StubbingOpsCats
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, NOT_IMPLEMENTED}
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.common.AlcoholDutyTestData
import uk.gov.hmrc.alcoholdutyaccount.connectors.{FinancialDataConnector, ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus.Approved
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Beer, FinancialTransaction, FinancialTransactionDocument, FinancialTransactionItem, Obligation, ObligationData, ObligationDetails, Open, SubscriptionSummary}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class AlcoholDutyServiceSpec extends SpecBase with AlcoholDutyTestData {
  "AlcoholDutyService" - {
    val alcoholDutyReference: String = generateAlcoholDutyReference().sample.get

    "getSubscriptionSummary should" - {
      "return summary data from the connector when successful" in new SetUp {
        when(subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Right(approvedSubscriptionSummary)))
        whenReady(service.getSubscriptionSummary(alcoholDutyReference).value) {
          _ mustBe Right(approvedAdrSubscriptionSummary)
        }
      }

      "return an error if the connector is unable to obtain obligation data or an error occurred" in new SetUp {
        val error = ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")
        when(subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Left(error)))
        whenReady(service.getSubscriptionSummary(alcoholDutyReference).value) {
          _ mustBe Left(error)
        }
      }
    }

    "getObligations should" - {
      "return obligation data from the connector where one open return matches the period key" in new SetUp {
        when(obligationDataConnector.getOpenObligationDetails(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Right(obligationDataMultipleOpen)))
        whenReady(service.getOpenObligations(alcoholDutyReference, periodKey).value) {
          _ mustBe Right(adrObligationDetails)
        }
      }

      "return obligation data from the connector where one fulfilled return matches the period key" in new SetUp {
        when(obligationDataConnector.getOpenObligationDetails(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Right(obligationDataSingleFulfilled)))
        whenReady(service.getOpenObligations(alcoholDutyReference, periodKey).value) {
          _ mustBe Right(adrObligationDetailsFulfilled)
        }
      }

      "return NOT_FOUND where no return matches the period key" in new SetUp {
        when(obligationDataConnector.getOpenObligationDetails(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Right(obligationDataMultipleOpen)))
        whenReady(service.getOpenObligations(alcoholDutyReference, periodKey4).value) {
          _ mustBe Left(ErrorResponse(NOT_FOUND, "Obligation details not found for period key 24AH"))
        }
      }

      "return an error if the connector is unable to obtain obligation data" in new SetUp {
        val error = ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")
        when(obligationDataConnector.getOpenObligationDetails(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Left(error)))
        whenReady(service.getOpenObligations(alcoholDutyReference, periodKey).value)(_ mustBe Left(error))
      }
    }

    "extractReturns should" - {
      "return an empty Returns object when there are no obligation details" in new SetUp {
        val result = service.extractReturns(Seq.empty)
        result mustBe Returns()
      }

      "return a Returns object with a dueReturnExists true if the due date is in a week" in new SetUp {
        val obligationDetailsDueNextWeek =
          obligationDetails.copy(inboundCorrespondenceDueDate = LocalDate.now().plusDays(7))
        val result                       = service.extractReturns(Seq(obligationDetailsDueNextWeek))

        result mustBe Returns(
          dueReturnExists = Some(true),
          numberOfOverdueReturns = Some(0)
        )
      }

      "return a Returns object with dueReturnExists true if the due date is today" in new SetUp {
        val obligationDetailsDueToday = obligationDetails.copy(inboundCorrespondenceDueDate = LocalDate.now())
        val result                    = service.extractReturns(Seq(obligationDetailsDueToday))

        result mustBe Returns(
          dueReturnExists = Some(true),
          numberOfOverdueReturns = Some(0)
        )
      }

      "return a Returns object with dueReturnExists false and the number of overdue returns equals 1 if the due date was yesterday" in new SetUp {
        val obligationDetailsDueYesterday =
          obligationDetails.copy(inboundCorrespondenceDueDate = LocalDate.now().minusDays(1))
        val result                        = service.extractReturns(Seq(obligationDetailsDueYesterday))

        result mustBe Returns(
          dueReturnExists = Some(false),
          numberOfOverdueReturns = Some(1)
        )
      }
    }

    "extractPayments should" - {
      "return an empty Payments object if there are no financial transactions" in new SetUp {
        val result = service.extractPayments(emptyFinancialDocument)
        result mustBe Payments()
      }

      "return a Payments object with a charging reference for a single financial transaction" in new SetUp {
        val result = service.extractPayments(financialDocument)
        result mustBe Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = 100.00,
              isMultiplePaymentDue = false,
              chargeReference = Some("X1234567890")
            )
          )
        )
      }

      "return a Payments object without a charging reference for multiple financial transactions" in new SetUp {
        val financialDocumentMultipleTransactions = FinancialTransactionDocument(
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
        val result                                = service.extractPayments(financialDocumentMultipleTransactions)
        result mustBe Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = 350.00,
              isMultiplePaymentDue = true,
              chargeReference = None
            )
          )
        )
      }
    }

    "getReturnDetails should" - {
      "return None if the obligationDataConnector returns an error" in new SetUp {
        when(obligationDataConnector.getOpenObligationDetails(*)(*))
          .thenReturn(EitherT.fromEither(Left(ErrorResponse(NOT_FOUND, ""))))

        val result = service.getReturnDetails(alcoholDutyReference)
        result onComplete {
          case Success(value) => value mustBe None
          case _              => fail("Expected a successful future")
        }
      }

      "return a Returns object if the obligationDataConnector returns an obligation" in new SetUp {
        val obligationDataOneDue = ObligationData(obligations = Seq.empty)
        when(obligationDataConnector.getOpenObligationDetails(*)(*))
          .thenReturn(EitherT.fromEither(Right(obligationDataOneDue)))

        service.getReturnDetails(alcoholDutyReference).onComplete { result =>
          result mustBe Success(Some(Returns()))
        }
      }
    }

    "getPaymentInformation should" - {
      "return None if the financialDataConnector returns None" in new SetUp {
        when(financialDataConnector.getFinancialData(*)(*))
          .thenReturn(OptionT.none[Future, FinancialTransactionDocument])

        val result = service.getPaymentInformation(alcoholDutyReference)
        result onComplete {
          case Success(value) => value mustBe None
          case _              => fail("Expected a successful future")
        }
      }

      "return a empty Payments object if the financialDataConnector returns an empty Document" in new SetUp {
        financialDataConnector.getFinancialData(*)(*) returnsF emptyFinancialDocument

        service.getPaymentInformation(alcoholDutyReference).onComplete { result =>
          result mustBe Success(Some(Payments()))
        }
      }
    }

    "getAlcoholDutyCardData should" - {
      "return a Returns and Payments object if the obligationDataConnector and financialDataConnector return data" in new SetUp {
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
        when(obligationDataConnector.getOpenObligationDetails(*)(*))
          .thenReturn(EitherT.fromEither(Right(obligationDataOneDue)))

        financialDataConnector.getFinancialData(*)(*) returnsF emptyFinancialDocument

        whenReady(service.getAlcoholDutyCardData(alcoholDutyReference).value) { result =>
          result mustBe Right(
            AlcoholDutyCardData(
              alcoholDutyReference,
              Approved,
              false,
              false,
              Returns(Some(true), Some(0)),
              Payments()
            )
          )
        }
      }

      "return a Returns empty object if the obligationDataConnector return an error" in new SetUp {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = false,
          approvalStatus = hods.Approved,
          insolvencyFlag = false
        )
        subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

        when(obligationDataConnector.getOpenObligationDetails(*)(*))
          .thenReturn(EitherT.fromEither(Left(ErrorResponse(NOT_FOUND, ""))))

        financialDataConnector.getFinancialData(*)(*) returnsF emptyFinancialDocument

        whenReady(service.getAlcoholDutyCardData(alcoholDutyReference).value) { result =>
          result mustBe Right(
            AlcoholDutyCardData(alcoholDutyReference, Approved, true, false, Returns(), Payments())
          )
        }
      }

      "return a Payments empty object if the financialDataConnector return an error" in new SetUp {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = false,
          approvalStatus = hods.Approved,
          insolvencyFlag = false
        )
        subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

        val obligationDataOneDue = ObligationData(obligations = Seq.empty)
        when(obligationDataConnector.getOpenObligationDetails(*)(*))
          .thenReturn(EitherT.fromEither(Right(obligationDataOneDue)))

        financialDataConnector.getFinancialData(*)(*) returns OptionT.none[Future, FinancialTransactionDocument]

        whenReady(service.getAlcoholDutyCardData(alcoholDutyReference).value) { result =>
          result mustBe Right(
            AlcoholDutyCardData(alcoholDutyReference, Approved, false, true, Returns(), Payments())
          )
        }
      }

      "return an Insolvent Card Data if the subscription summary has insolvencyFlag set to true" in new SetUp {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = false,
          approvalStatus = hods.Approved,
          insolvencyFlag = true
        )
        subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

        whenReady(service.getAlcoholDutyCardData(alcoholDutyReference).value) { result =>
          result mustBe Right(
            InsolventCardData(alcoholDutyReference)
          )
        }
      }

      "return an error if the subscription summary return an error" in new SetUp {
        val error = ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred")

        when(subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference))
          .thenReturn(EitherT.fromEither(Left(error)))

        whenReady(service.getAlcoholDutyCardData(alcoholDutyReference).value) { result =>
          result mustBe Left(error)
        }
      }

      Seq(hods.Revoked, hods.DeRegistered).foreach { approvalStatus =>
        s"return a NOT_IMPLEMENTED error if the subscription summary return a $approvalStatus type" in new SetUp {
          val subscriptionSummary = SubscriptionSummary(
            typeOfAlcoholApprovedForList = Set(Beer),
            smallCiderFlag = false,
            approvalStatus = approvalStatus,
            insolvencyFlag = false
          )
          subscriptionSummaryConnector.getSubscriptionSummary(*)(*) returnsF subscriptionSummary

          whenReady(service.getAlcoholDutyCardData(alcoholDutyReference).value) { result =>
            result mustBe Left(
              ErrorResponse(NOT_IMPLEMENTED, "Approval Status not yet supported")
            )
          }
        }
      }
    }
  }

  class SetUp extends AlcoholDutyTestData {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val hc: HeaderCarrier    = HeaderCarrier()

    val subscriptionSummaryConnector = mock[SubscriptionSummaryConnector]
    val obligationDataConnector      = mock[ObligationDataConnector]
    val financialDataConnector       = mock[FinancialDataConnector]
    val service                      = new AlcoholDutyService(subscriptionSummaryConnector, obligationDataConnector, financialDataConnector)

    val periodStart = LocalDate.of(2023, 1, 1)
    val periodEnd   = LocalDate.of(2023, 1, 31)
  }
}

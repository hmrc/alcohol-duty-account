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

import org.mockito.Mockito.when
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.alcoholdutyaccount.models.payments._
import uk.gov.hmrc.alcoholdutyaccount.models.{ErrorCodes, ReturnPeriod}
import uk.gov.hmrc.alcoholdutyaccount.utils.payments.PaymentsValidator
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.concurrent.Future

class OpenPaymentsServiceSpec extends SpecBase {
  "PaymentsService" - {
    "when calling getOpenPayments" - {
      "a successful and correct response must be returned" - {
        "handle no financial data (nil return or no data)" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(emptyFinancialDocument)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                Seq.empty,
                BigDecimal("0"),
                Seq.empty,
                BigDecimal("0"),
                BigDecimal("0")
              )
            )
          }
        }

        "when processing a single fully outstanding line item for a return" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleFullyOutstandingReturn)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.Return,
                    dueDate = singleFullyOutstandingReturn.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singleFullyOutstandingReturn.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("9000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("9000"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("9000")
              )
            )
          }
        }

        "when processing a single partially outstanding line item for a return" in new SetUp {
          val financialTransactionDocument = singlePartiallyOutstandingReturn(onlyOpenItems = true)

          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(financialTransactionDocument)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.Return,
                    dueDate = financialTransactionDocument.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = financialTransactionDocument.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("5000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("5000"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("5000")
              )
            )
          }
        }

        "when processing a two line item outstanding return" in new SetUp {
          val financialTransactionDocument = twoLineItemPartiallyOutstandingReturn(onlyOpenItems = true)

          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(financialTransactionDocument)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.Return,
                    dueDate = financialTransactionDocument.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = financialTransactionDocument.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("7000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("7000"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("7000")
              )
            )
          }
        }

        "when processing two outstanding returns" in new SetUp {
          val financialTransactionDocument = twoSeparateOutstandingReturnsOnePartiallyPaid(onlyOpenItems = true)

          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(financialTransactionDocument)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            case Left(_)             => fail()
            case Right(openPayments) =>
              openPayments.outstandingPayments        must contain theSameElementsAs Seq(
                OutstandingPayment(
                  taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                  taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                  transactionType = TransactionType.Return,
                  dueDate = financialTransactionDocument.financialTransactions(0).items.head.dueDate.get,
                  chargeReference = financialTransactionDocument.financialTransactions(0).chargeReference,
                  remainingAmount = BigDecimal("5000")
                ),
                OutstandingPayment(
                  taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodFromDate()),
                  taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodToDate()),
                  transactionType = TransactionType.Return,
                  dueDate = financialTransactionDocument.financialTransactions(1).items.head.dueDate.get,
                  chargeReference = financialTransactionDocument.financialTransactions(1).chargeReference,
                  remainingAmount = BigDecimal("2000")
                )
              )
              openPayments.totalOutstandingPayments mustBe BigDecimal("7000")
              openPayments.unallocatedPayments      mustBe Seq.empty
              openPayments.totalUnallocatedPayments mustBe BigDecimal("0")
              openPayments.totalOpenPaymentsAmount  mustBe BigDecimal("7000")
          }
        }

        "must ignore any cleared payments returned despite an API call for only open items" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(clearedPaymentsReturnedAsOpen)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.Return,
                    dueDate = singleFullyOutstandingReturn.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singleFullyOutstandingReturn.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("9000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("9000"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = singleOverpayment.financialTransactions.head.items.head.dueDate.get,
                    unallocatedAmount = BigDecimal("-9000")
                  )
                ),
                totalUnallocatedPayments = BigDecimal("-9000"),
                totalOpenPaymentsAmount = BigDecimal("0")
              )
            )
          }
        }

        "must ignore overpayments that have no contract object type" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleOverpaymentNoContractObjectType)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq.empty,
                totalOutstandingPayments = BigDecimal("0"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("0")
              )
            )
          }
        }

        "must ignore overpayments that are not contract object type ZADP" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleOverpaymentNotZADP)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq.empty,
                totalOutstandingPayments = BigDecimal("0"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("0")
              )
            )
          }
        }

        "when processing a single fully unallocated overpayment" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleOverpayment)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq.empty,
                totalOutstandingPayments = BigDecimal("0"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = singleOverpayment.financialTransactions.head.items.head.dueDate.get,
                    unallocatedAmount = BigDecimal("-9000")
                  )
                ),
                totalUnallocatedPayments = BigDecimal("-9000"),
                totalOpenPaymentsAmount = BigDecimal("-9000")
              )
            )
          }
        }

        "must warn when processing a single fully unallocated overpayment where original and outstanding amounts don't match (coverage)" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleOverpaymentAmountMismatch)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq.empty,
                totalOutstandingPayments = BigDecimal("0"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = singleOverpayment.financialTransactions.head.items.head.dueDate.get,
                    unallocatedAmount = BigDecimal("-1000")
                  )
                ),
                totalUnallocatedPayments = BigDecimal("-1000"),
                totalOpenPaymentsAmount = BigDecimal("-1000")
              )
            )
          }
        }

        "when processing two separate overpayments" in new SetUp { // This probably won't happen, but check it can be handled
          val twoSeparateOverpaymentsOpen = twoSeparateOverpayments(true)

          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(twoSeparateOverpaymentsOpen)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            case Left(_)             => fail()
            case Right(openPayments) =>
              openPayments.outstandingPayments      mustBe Seq.empty
              openPayments.totalOutstandingPayments mustBe BigDecimal("0")
              openPayments.unallocatedPayments        must contain theSameElementsAs Seq(
                UnallocatedPayment(
                  paymentDate = twoSeparateOverpaymentsOpen.financialTransactions(0).items.head.dueDate.get,
                  unallocatedAmount = BigDecimal("-5000")
                ),
                UnallocatedPayment(
                  paymentDate = twoSeparateOverpaymentsOpen.financialTransactions(1).items.head.dueDate.get,
                  unallocatedAmount = BigDecimal("-2000")
                )
              )
              openPayments.totalUnallocatedPayments mustBe BigDecimal("-7000")
              openPayments.totalOpenPaymentsAmount  mustBe BigDecimal("-7000")
          }
        }

        "when processing a single partially outstanding return and a partially allocated overpayment" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(
              Future.successful(
                Right(
                  onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedOverpayment
                )
              )
            )

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.Return,
                    dueDate =
                      onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedOverpayment.financialTransactions.head.items.head.dueDate.get,
                    chargeReference =
                      onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedOverpayment.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("5000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("5000"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedOverpayment
                      .financialTransactions(1)
                      .items
                      .head
                      .dueDate
                      .get,
                    unallocatedAmount = BigDecimal("-2000")
                  )
                ),
                totalUnallocatedPayments = BigDecimal("-2000"),
                totalOpenPaymentsAmount = BigDecimal("3000")
              )
            )
          }
        }

        "when processing a single fully outstanding LPI" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleFullyOutstandingLPI)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.LPI,
                    dueDate = singleFullyOutstandingLPI.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singleFullyOutstandingLPI.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("50")
                  )
                ),
                totalOutstandingPayments = BigDecimal("50"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("50")
              )
            )
          }
        }

        "when processing a single fully outstanding RPI" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleRPI)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = None,
                    taxPeriodTo = None,
                    transactionType = TransactionType.RPI,
                    dueDate = singleRPI.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singleRPI.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("-50")
                  )
                ),
                totalOutstandingPayments = BigDecimal("-50"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("-50")
              )
            )
          }
        }

        "when processing a single fully outstanding Central Assessment charge" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleCA)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.CA,
                    dueDate = singleCA.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singleCA.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("2000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("2000"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("2000")
              )
            )
          }
        }

        "when processing a single fully outstanding Central Assessment Interest charge" in new SetUp {
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Right(singleCAI)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                    taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
                    transactionType = TransactionType.CAI,
                    dueDate = singleCAI.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singleCAI.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("20")
                  )
                ),
                totalOutstandingPayments = BigDecimal("20"),
                unallocatedPayments = Seq.empty,
                totalUnallocatedPayments = BigDecimal("0"),
                totalOpenPaymentsAmount = BigDecimal("20")
              )
            )
          }
        }
      }

      "an error must be returned" - {
        "if getting financial data returns an error" in new SetUp {
          val errorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "error!")
          when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
            .thenReturn(Future.successful(Left(errorResponse)))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            _ mustBe Left(errorResponse)
          }
        }
      }

      "if no items are present on the first line item" in new SetUp {
        val noItemsOnFinancialDocument = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(singleFullyOutstandingReturn.financialTransactions.head.copy(items = Seq.empty))
        )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(noItemsOnFinancialDocument)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if no due date is present on the first item of the first line item of a document" in new SetUp {
        val noDueDatePresentOnFirstItem = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(
            singleFullyOutstandingReturn.financialTransactions.head.copy(items =
              Seq(singleFullyOutstandingReturn.financialTransactions.head.items.head.copy(dueDate = None))
            )
          )
        )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(noDueDatePresentOnFirstItem)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if no items are present on the subsequent line items" in new SetUp {
        val noItemsOnSecondLineItem = twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(1).copy(items = Seq.empty)
          )
        )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(noItemsOnSecondLineItem)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if all transaction types on a document aren't the same" in new SetUp {
        val mismatchedTransactionTypesOnLineItems =
          twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0),
              twoLineItemPartiallyOutstandingReturnOpen
                .financialTransactions(1)
                .copy(mainTransaction = TransactionType.toMainTransactionType(TransactionType.LPI))
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(mismatchedTransactionTypesOnLineItems)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if all period key is there on some, but not all document entries" in new SetUp {
        val mismatchedTransactionTypesOnLineItems =
          twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0),
              twoLineItemPartiallyOutstandingReturnOpen
                .financialTransactions(1)
                .copy(periodKey = None)
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(mismatchedTransactionTypesOnLineItems)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if all period keys are present, but do not match on all document line items" in new SetUp {
        val mismatchedTransactionTypesOnLineItems =
          twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0),
              twoLineItemPartiallyOutstandingReturnOpen
                .financialTransactions(1)
                .copy(periodKey = Some(periodKey2))
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(mismatchedTransactionTypesOnLineItems)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if chargeReference is missing on the first, but present on the second line item of a document" in new SetUp {
        val missingChargeReferencesOnSecondLineItem =
          twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0).copy(chargeReference = None),
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(1)
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(missingChargeReferencesOnSecondLineItem)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if chargeReference is present on the first, but not the second line item of a document" in new SetUp {
        val missingChargeReferencesOnSecondLineItem =
          twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0),
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(1).copy(chargeReference = None)
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(missingChargeReferencesOnSecondLineItem)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if all chargeReferences on a document aren't the same if present on the first" in new SetUp {
        val mismatchedChargeReferencesOnLineItems =
          twoLineItemPartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0),
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(1).copy(chargeReference = Some("blah"))
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(mismatchedChargeReferencesOnLineItems)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if no due date is present on any subsequent items of a document" in new SetUp {
        val noDueDatePresentOnSecondItemOfDocument = singlePartiallyOutstandingReturnOpen.copy(financialTransactions =
          Seq(
            singlePartiallyOutstandingReturnOpen.financialTransactions.head.copy(items =
              Seq(
                singlePartiallyOutstandingReturnOpen.financialTransactions.head.items.head,
                singlePartiallyOutstandingReturnOpen.financialTransactions.head.items.head.copy(dueDate = None)
              )
            )
          )
        )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(noDueDatePresentOnSecondItemOfDocument)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if any due date on any subsequent items of a document doesn't match the first" in new SetUp {
        val mismatchedDueDatePresentOnSecondItemOfDocument =
          singlePartiallyOutstandingReturnOpen.copy(financialTransactions =
            Seq(
              singlePartiallyOutstandingReturnOpen.financialTransactions.head.copy(items =
                Seq(
                  singlePartiallyOutstandingReturnOpen.financialTransactions.head.items.head,
                  singlePartiallyOutstandingReturnOpen.financialTransactions.head.items.head
                    .copy(dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate()))
                )
              )
            )
          )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(mismatchedDueDatePresentOnSecondItemOfDocument)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if the document has an unknown [to ADR] transaction type" in new SetUp {
        val badTransactionType     = "1111"
        val unknownTransactionType = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(singleFullyOutstandingReturn.financialTransactions.head.copy(mainTransaction = badTransactionType))
        )

        when(mockFinancialDataConnector.getOnlyOpenFinancialData(appaId))
          .thenReturn(Future.successful(Right(unknownTransactionType)))

        whenReady(paymentsService.getOpenPayments(appaId).value) {
          _ mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }
    }

    "when calling calculateOutstandingAmount" - {
      "and outstanding amount is missing for some reason" - {
        "the total can be calculated by assuming it is 0 (coverage)" in new SetUp {
          paymentsService.calculateOutstandingAmount(
            Seq(
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(0).copy(outstandingAmount = None),
              twoLineItemPartiallyOutstandingReturnOpen.financialTransactions(1)
            )
          ) mustBe BigDecimal("2000")
        }
      }
    }
  }

  class SetUp {
    val mockFinancialDataConnector                = mock[FinancialDataConnector]
    val paymentsValidator: PaymentsValidator      = new PaymentsValidator()
    val paymentsService                           = new OpenPaymentsService(mockFinancialDataConnector, paymentsValidator)
    val year                                      = 2024
    val singlePartiallyOutstandingReturnOpen      = singlePartiallyOutstandingReturn(onlyOpenItems = true)
    val twoLineItemPartiallyOutstandingReturnOpen = twoLineItemPartiallyOutstandingReturn(onlyOpenItems = true)

    val singleOverpaymentAmountMismatch = singleOverpayment.copy(financialTransactions =
      singleOverpayment.financialTransactions.map(_.copy(outstandingAmount = Some(BigDecimal("-1000"))))
    )

    val singleOverpaymentNoContractObjectType = singleOverpayment.copy(financialTransactions =
      singleOverpayment.financialTransactions.map(_.copy(contractObjectType = None))
    )

    val singleOverpaymentNotZADP = singleOverpayment.copy(financialTransactions =
      singleOverpayment.financialTransactions.map(_.copy(contractObjectType = Some("blah")))
    )

    val onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedOverpayment: FinancialTransactionDocument =
      combineFinancialTransactionDocuments(
        Seq(
          createFinancialDocument(
            onlyOpenItems = true,
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            originalAmount = BigDecimal("9000"),
            maybeOutstandingAmount = Some(BigDecimal("5000")),
            dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
            transactionType = TransactionType.Return,
            maybePeriodKey = Some(periodKey),
            maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
            maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
            maybeChargeReference = Some(chargeReferenceGen.sample.get)
          ),
          createFinancialDocument(
            onlyOpenItems = true,
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            originalAmount = BigDecimal("-2000"),
            maybeOutstandingAmount = Some(BigDecimal("-2000")),
            dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
            transactionType = TransactionType.Overpayment,
            maybeChargeReference = None
          )
        )
      )

    val clearedPaymentsReturnedAsOpen: FinancialTransactionDocument =
      combineFinancialTransactionDocuments(
        Seq(
          singleFullyOutstandingReturn,
          createFinancialDocument(
            onlyOpenItems = true,
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            originalAmount = BigDecimal("9000"),
            maybeOutstandingAmount = None,
            dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
            transactionType = TransactionType.Return,
            maybePeriodKey = Some(periodKey),
            maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
            maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
            maybeChargeReference = Some(chargeReference)
          ),
          createFinancialDocument(
            onlyOpenItems = true,
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            originalAmount = BigDecimal("50"),
            maybeOutstandingAmount = None,
            dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
            transactionType = TransactionType.LPI,
            maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
            maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
            maybeChargeReference = Some(chargeReference)
          ),
          singleOverpayment
        )
      )
  }
}

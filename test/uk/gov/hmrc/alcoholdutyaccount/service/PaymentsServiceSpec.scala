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

import cats.data.EitherT
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.common.ReturnPeriod
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionDocument, FinancialTransactionItem}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{OpenPayments, OutstandingPayment, TransactionType, UnallocatedPayment}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class PaymentsServiceSpec extends SpecBase {
  "PaymentsService" - {
    "when calling getOpenPayments" - {
      "a successful and correct response should be returned" - {
        "when processing a single fully outstanding line item for a return" in new SetUp {
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleFullyOutstandingReturn))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
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
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singlePartiallyOutstandingReturn))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    transactionType = TransactionType.Return,
                    dueDate = singlePartiallyOutstandingReturn.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = singlePartiallyOutstandingReturn.financialTransactions.head.chargeReference,
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
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoLineItemPartiallyOutstandingReturn))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    transactionType = TransactionType.Return,
                    dueDate = twoLineItemPartiallyOutstandingReturn.financialTransactions.head.items.head.dueDate.get,
                    chargeReference = twoLineItemPartiallyOutstandingReturn.financialTransactions.head.chargeReference,
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
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoSeparateOutstandingReturnsOnePartiallyPaid))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            case Left(_)             => fail()
            case Right(openPayments) =>
              openPayments.outstandingPayments must contain theSameElementsAs Seq(
                OutstandingPayment(
                  transactionType = TransactionType.Return,
                  dueDate =
                    twoSeparateOutstandingReturnsOnePartiallyPaid.financialTransactions(0).items.head.dueDate.get,
                  chargeReference =
                    twoSeparateOutstandingReturnsOnePartiallyPaid.financialTransactions(0).chargeReference,
                  remainingAmount = BigDecimal("5000")
                ),
                OutstandingPayment(
                  transactionType = TransactionType.Return,
                  dueDate =
                    twoSeparateOutstandingReturnsOnePartiallyPaid.financialTransactions(1).items.head.dueDate.get,
                  chargeReference =
                    twoSeparateOutstandingReturnsOnePartiallyPaid.financialTransactions(1).chargeReference,
                  remainingAmount = BigDecimal("2000")
                )
              )
              openPayments.totalOutstandingPayments mustBe BigDecimal("7000")
              openPayments.unallocatedPayments mustBe Seq.empty
              openPayments.totalUnallocatedPayments mustBe BigDecimal("0")
              openPayments.totalOpenPaymentsAmount mustBe BigDecimal("7000")
          }
        }

        "when processing a single fully unallocated payment on account" in new SetUp {
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singlePaymentOnAccount))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq.empty,
                totalOutstandingPayments = BigDecimal("0"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = singlePaymentOnAccount.financialTransactions.head.items.head.dueDate.get,
                    unallocatedAmount = BigDecimal("-9000")
                  )
                ),
                totalUnallocatedPayments = BigDecimal("-9000"),
                totalOpenPaymentsAmount = BigDecimal("-9000")
              )
            )
          }
        }

        "when processing two separate payments on account" in new SetUp { // This probably won't happen, but check it can be handled
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoSeparatePaymentsOnAccount))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            case Left(_)             => fail()
            case Right(openPayments) =>
              openPayments.outstandingPayments mustBe Seq.empty
              openPayments.totalOutstandingPayments mustBe BigDecimal("0")
              openPayments.unallocatedPayments must contain theSameElementsAs Seq(
                UnallocatedPayment(
                  paymentDate = twoSeparatePaymentsOnAccount.financialTransactions(0).items.head.dueDate.get,
                  unallocatedAmount = BigDecimal("-5000")
                ),
                UnallocatedPayment(
                  paymentDate = twoSeparatePaymentsOnAccount.financialTransactions(1).items.head.dueDate.get,
                  unallocatedAmount = BigDecimal("-2000")
                )
              )
              openPayments.totalUnallocatedPayments mustBe BigDecimal("-7000")
              openPayments.totalOpenPaymentsAmount mustBe BigDecimal("-7000")
          }
        }

        // This is a test edge case which shouldn't happen in real life, to check the outstanding amount is used from payment on account
        "when processing a single partially outstanding return and a partially allocated payment on account" in new SetUp {
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(
              EitherT.pure[Future, ErrorResponse](
                onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedPaymentOnAccount
              )
            )

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
                    transactionType = TransactionType.Return,
                    dueDate =
                      onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedPaymentOnAccount.financialTransactions.head.items.head.dueDate.get,
                    chargeReference =
                      onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedPaymentOnAccount.financialTransactions.head.chargeReference,
                    remainingAmount = BigDecimal("5000")
                  )
                ),
                totalOutstandingPayments = BigDecimal("5000"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedPaymentOnAccount
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
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleFullyOutstandingLPI))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
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

        "when processing a single fully outstanding RPI" in new SetUp { // This shouldn't appear as an open payment
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleRPI))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq(
                  OutstandingPayment(
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
      }

      "an error should be returned" - {
        "if getting financial data returns an error" in new SetUp {
          val errorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "error!")
          when(mockFinancialDataConnector.getFinancialData(appaId))
            .thenReturn(EitherT.leftT[Future, FinancialTransactionDocument](errorResponse))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Left(errorResponse)
          }
        }
      }

      "if no items are present on the first line item" in new SetUp {
        val noItemsOnFinancialDocument = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(singleFullyOutstandingReturn.financialTransactions.head.copy(items = Seq.empty))
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noItemsOnFinancialDocument))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
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

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noDueDatePresentOnFirstItem))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if no items are present on the subsequent line items" in new SetUp {
        val noItemsOnFinancialDocument = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturn.financialTransactions(1).copy(items = Seq.empty)
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noItemsOnFinancialDocument))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if all transaction types on a document aren't the same" in new SetUp {
        val mismatchedTransactionTypesOnLineItems = twoLineItemPartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturn
              .financialTransactions(1)
              .copy(mainTransaction = TransactionType.toMainTransactionType(TransactionType.LPI))
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](mismatchedTransactionTypesOnLineItems))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if chargeReference is missing on the first, but present on the second line item of a document" in new SetUp {
        val missingChargeReferencesOnSecondLineItem = twoLineItemPartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0).copy(chargeReference = None),
            twoLineItemPartiallyOutstandingReturn.financialTransactions(1)
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](missingChargeReferencesOnSecondLineItem))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if chargeReference is present on the first, but not the second line item of a document" in new SetUp {
        val missingChargeReferencesOnSecondLineItem = twoLineItemPartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturn.financialTransactions(1).copy(chargeReference = None)
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](missingChargeReferencesOnSecondLineItem))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if all chargeReferences on a document aren't the same if present on the first" in new SetUp {
        val mismatchedChargeReferencesOnLineItems = twoLineItemPartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturn.financialTransactions(1).copy(chargeReference = Some("blah"))
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](mismatchedChargeReferencesOnLineItems))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if no due date is present on any subsequent items of a document" in new SetUp {
        val noDueDatePresentOnSecondItemOfDocument = singlePartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            singlePartiallyOutstandingReturn.financialTransactions.head.copy(items =
              Seq(
                singlePartiallyOutstandingReturn.financialTransactions.head.items.head,
                singlePartiallyOutstandingReturn.financialTransactions.head.items.head.copy(dueDate = None)
              )
            )
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noDueDatePresentOnSecondItemOfDocument))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if any due date on any subsequent items of a document doesn't match the first" in new SetUp {
        val mismatchedDueDatePresentOnSecondItemOfDocument =
          singlePartiallyOutstandingReturn.copy(financialTransactions =
            Seq(
              singlePartiallyOutstandingReturn.financialTransactions.head.copy(items =
                Seq(
                  singlePartiallyOutstandingReturn.financialTransactions.head.items.head,
                  singlePartiallyOutstandingReturn.financialTransactions.head.items.head
                    .copy(dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate()))
                )
              )
            )
          )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](mismatchedDueDatePresentOnSecondItemOfDocument))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }

      "if the document has an unknown [to ADR] transaction type" in new SetUp {
        val badTransactionType     = "1111"
        val unknownTransactionType = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(singleFullyOutstandingReturn.financialTransactions.head.copy(mainTransaction = badTransactionType))
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](unknownTransactionType))

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
        }
      }
    }

    "when calling calculateOutstandingAmount" - {
      "and outstanding amount is missing for some reason" - {
        "the total can be calculated by assuming it is 0 (coverage)" in new SetUp {
          paymentsService.calculateOutstandingAmount(
            Seq(
              twoLineItemPartiallyOutstandingReturn.financialTransactions(0).copy(outstandingAmount = None),
              twoLineItemPartiallyOutstandingReturn.financialTransactions(1)
            )
          ) mustBe BigDecimal("2000")
        }
      }
    }

    "when calling validateAndGetFinancialTransactionData" - {
      "and there is no financial transactions for a document which should not be possible" - {
        "it should return an error gracefully (coverage)" in new SetUp {
          val sapDocumentNumber = sapDocumentNumberGen.sample.get

          paymentsService.validateAndGetFinancialTransactionData(sapDocumentNumber, Seq.empty) mustBe Left(
            ErrorCodes.unexpectedResponse
          )
        }
      }
    }
  }

  class SetUp {
    val mockFinancialDataConnector = mock[FinancialDataConnector]
    val paymentsService            = new PaymentsService(mockFinancialDataConnector)

    // Test edge case which shouldn't happen as payments on account should reduce original amount
    val onePartiallyPaidReturnLineItemAndOnePartiallyAllocatedPaymentOnAccount: FinancialTransactionDocument =
      FinancialTransactionDocument(
        financialTransactions = Seq(
          FinancialTransaction(
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            periodKey = Some(periodKey),
            chargeReference = Some(chargeReferenceGen.sample.get),
            originalAmount = BigDecimal("9000"),
            mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
            subTransaction = "6132",
            outstandingAmount = Some(BigDecimal("5000")),
            items = Seq(
              FinancialTransactionItem(
                subItem = "000",
                dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
                amount = BigDecimal("5000")
              )
            )
          ),
          FinancialTransaction(
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            periodKey = None,
            chargeReference = None,
            originalAmount = BigDecimal("-2000"),
            mainTransaction = TransactionType.toMainTransactionType(TransactionType.PaymentOnAccount),
            subTransaction = "6132",
            outstandingAmount = Some(BigDecimal("-2000")),
            items = Seq(
              FinancialTransactionItem(
                subItem = "000",
                dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
                amount = BigDecimal("-2000")
              )
            )
          )
        )
      )
  }
}

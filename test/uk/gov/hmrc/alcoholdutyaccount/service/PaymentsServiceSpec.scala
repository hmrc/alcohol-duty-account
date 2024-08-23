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
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.ReturnPeriod
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{HistoricPayment, HistoricPayments, OpenPayments, OutstandingPayment, TransactionType, UnallocatedPayment}
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
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoSeparateReturnsOneOutstanding))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            case Left(_)             => fail()
            case Right(openPayments) =>
              openPayments.outstandingPayments must contain theSameElementsAs Seq(
                OutstandingPayment(
                  transactionType = TransactionType.Return,
                  dueDate = twoSeparateReturnsOneOutstanding.financialTransactions(0).items.head.dueDate.get,
                  chargeReference = twoSeparateReturnsOneOutstanding.financialTransactions(0).chargeReference,
                  remainingAmount = BigDecimal("5000")
                ),
                OutstandingPayment(
                  transactionType = TransactionType.Return,
                  dueDate = twoSeparateReturnsOneOutstanding.financialTransactions(1).items.head.dueDate.get,
                  chargeReference = twoSeparateReturnsOneOutstanding.financialTransactions(1).chargeReference,
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
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleFullyUnallocatedPayment))

          whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
            result mustBe Right(
              OpenPayments(
                outstandingPayments = Seq.empty,
                totalOutstandingPayments = BigDecimal("0"),
                unallocatedPayments = Seq(
                  UnallocatedPayment(
                    paymentDate = singleFullyUnallocatedPayment.financialTransactions.head.items.head.dueDate.get,
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
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoSeparatePayments))

          whenReady(paymentsService.getOpenPayments(appaId).value) {
            case Left(_)             => fail()
            case Right(openPayments) =>
              openPayments.outstandingPayments mustBe Seq.empty
              openPayments.totalOutstandingPayments mustBe BigDecimal("0")
              openPayments.unallocatedPayments must contain theSameElementsAs Seq(
                UnallocatedPayment(
                  paymentDate = twoSeparatePayments.financialTransactions(0).items.head.dueDate.get,
                  unallocatedAmount = BigDecimal("-5000")
                ),
                UnallocatedPayment(
                  paymentDate = twoSeparatePayments.financialTransactions(1).items.head.dueDate.get,
                  unallocatedAmount = BigDecimal("-2000")
                )
              )
              openPayments.totalUnallocatedPayments mustBe BigDecimal("-7000")
              openPayments.totalOpenPaymentsAmount mustBe BigDecimal("-7000")
          }
        }

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
                    chargeReference = None,
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
                    chargeReference = None,
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

      "if no items are present on a document" in new SetUp {
        val noItemsOnFinancialDocument = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(singleFullyOutstandingReturn.financialTransactions.head.copy(items = Seq.empty))
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noItemsOnFinancialDocument))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Expected at least one item for financial transaction ${noItemsOnFinancialDocument.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }

      "if no due date is present on the first item of a document" in new SetUp {
        val noDueDatePresentOnFirstItem = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(
            singleFullyOutstandingReturn.financialTransactions.head.copy(items =
              Seq(singleFullyOutstandingReturn.financialTransactions.head.items.head.copy(dueDate = None))
            )
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noDueDatePresentOnFirstItem))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Due date not found on first item of first entry of financial transaction ${noDueDatePresentOnFirstItem.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }

      "if no due date is present on any subsequent items of a document" in new SetUp {
        val noDueDatePresentOnSecondItemOfDocument = singlePartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            singlePartiallyOutstandingReturn.financialTransactions.head.copy(items =
              Seq(
                singlePartiallyOutstandingReturn.financialTransactions.head.items(0),
                singlePartiallyOutstandingReturn.financialTransactions.head.items(1).copy(dueDate = None)
              )
            )
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](noDueDatePresentOnSecondItemOfDocument))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all dueDates matched for first entry of financial transaction ${noDueDatePresentOnSecondItemOfDocument.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }

      "if any due date on any subsequent items of a document doesn't match the first" in new SetUp {
        val mismatchedDueDatePresentOnSecondItemOfDocument =
          singlePartiallyOutstandingReturn.copy(financialTransactions =
            Seq(
              singlePartiallyOutstandingReturn.financialTransactions.head.copy(items =
                Seq(
                  singlePartiallyOutstandingReturn.financialTransactions.head.items(0),
                  singlePartiallyOutstandingReturn.financialTransactions.head
                    .items(1)
                    .copy(dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate()))
                )
              )
            )
          )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](mismatchedDueDatePresentOnSecondItemOfDocument))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all dueDates matched for first entry of financial transaction ${mismatchedDueDatePresentOnSecondItemOfDocument.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
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

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction ${mismatchedTransactionTypesOnLineItems.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
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

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction ${missingChargeReferencesOnSecondLineItem.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
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

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction ${missingChargeReferencesOnSecondLineItem.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
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

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction ${mismatchedChargeReferencesOnLineItems.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }

      "if dueDates are not present on subsequent line items" in new SetUp {
        val missingDueDateOnSecondLineItem = twoLineItemPartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturn
              .financialTransactions(1)
              .copy(items =
                Seq(twoLineItemPartiallyOutstandingReturn.financialTransactions(1).items.head.copy(dueDate = None))
              )
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](missingDueDateOnSecondLineItem))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction ${missingDueDateOnSecondLineItem.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }

      "if dueDates on subsequent items do not match the first" in new SetUp {
        val mismatchedDueDateOnSecondLineItem = twoLineItemPartiallyOutstandingReturn.copy(financialTransactions =
          Seq(
            twoLineItemPartiallyOutstandingReturn.financialTransactions(0),
            twoLineItemPartiallyOutstandingReturn
              .financialTransactions(1)
              .copy(items =
                Seq(
                  twoLineItemPartiallyOutstandingReturn
                    .financialTransactions(1)
                    .items
                    .head
                    .copy(dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate()))
                )
              )
          )
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](mismatchedDueDateOnSecondLineItem))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction ${mismatchedDueDateOnSecondLineItem.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }

      "if the document has an unknown [to ADR] transaction type" in new SetUp {
        val badTransactionType     = "1111"
        val unknownTransactionType = singleFullyOutstandingReturn.copy(financialTransactions =
          Seq(singleFullyOutstandingReturn.financialTransactions.head.copy(mainTransaction = badTransactionType))
        )

        when(mockFinancialDataConnector.getFinancialData(appaId))
          .thenReturn(EitherT.pure[Future, ErrorResponse](unknownTransactionType))

        val errorResponse = ErrorResponse(
          INTERNAL_SERVER_ERROR,
          s"Unexpected transaction type $badTransactionType on financial transaction ${unknownTransactionType.financialTransactions.head.sapDocumentNumber}."
        )

        whenReady(paymentsService.getOpenPayments(appaId).value) { result =>
          result mustBe Left(errorResponse)
        }
      }
    }

    "when calling getHistoricPayments" - {
      "a successful and correct response should be returned" - {
        "for multiple statuses" in new SetUp {
          when(mockFinancialDataConnector.getFinancialData(appaId = appaId, open = false, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](multipleStatuses))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            case Right(HistoricPayments(`year`, payments)) =>
              payments.map(payment =>
                payment.copy(chargeReference = payment.chargeReference.map(_ => "ChargeRef"))
              ) must contain theSameElementsAs Seq(
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AH"),
                  TransactionType.Return,
                  Some("ChargeRef"),
                  BigDecimal("237.44")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AE"),
                  TransactionType.Return,
                  Some("ChargeRef"),
                  BigDecimal("4577.44")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AD"),
                  TransactionType.Return,
                  Some("ChargeRef"),
                  BigDecimal("4577.44")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AC"),
                  TransactionType.Return,
                  Some("ChargeRef"),
                  BigDecimal("-4577.44")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AB"),
                  TransactionType.LPI,
                  Some("ChargeRef"),
                  BigDecimal("20.56")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AB"),
                  TransactionType.LPI,
                  Some("ChargeRef"),
                  BigDecimal("20.56")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AA"),
                  TransactionType.RPI,
                  None,
                  BigDecimal("-50.00")
                )
              )
            case _                                         => fail()
          }
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

    "when calling validateAndGetCommonData" - {
      "and there is no financial transactions for a document which should not be possible" - {
        "it should return an error gracefully (coverage)" in new SetUp {
          val sapDocumentNumber = sapDocumentNumberGen.sample.get

          paymentsService.validateAndGetCommonData(sapDocumentNumber, Seq.empty, open = true) mustBe Left(
            ErrorResponse(
              INTERNAL_SERVER_ERROR,
              s"Should have had a least one entry for financial transaction $sapDocumentNumber. This shouldn't happen"
            )
          )
        }
      }
    }
  }

  class SetUp {
    val mockFinancialDataConnector = mock[FinancialDataConnector]
    val paymentsService            = new PaymentsService(mockFinancialDataConnector)

    val year = 2024
  }
}

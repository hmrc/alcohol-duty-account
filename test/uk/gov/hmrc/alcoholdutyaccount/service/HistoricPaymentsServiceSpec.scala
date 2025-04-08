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
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.alcoholdutyaccount.models.payments._
import uk.gov.hmrc.alcoholdutyaccount.models.{ErrorCodes, ReturnPeriod}
import uk.gov.hmrc.alcoholdutyaccount.utils.payments.PaymentsValidator
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class HistoricPaymentsServiceSpec extends SpecBase {
  "PaymentsService" - {
    "when calling getHistoricPayments" - {
      "a successful and correct response must be returned" - {
        "handle no financial data (nil return or no data for the period)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](emptyFinancialDocument))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq.empty
              )
            )
          }
        }

        "when just overpayment (no historic payments returned)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoSeparateOverpayments(false)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(HistoricPayments(year, Seq.empty))
          }
        }

        "when just RPI which must be filtered as it's a negative amount" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleRPI))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(
              HistoricPayments(
                year,
                Seq.empty
              )
            )
          }
        }

        // This is a test edge case which mustn't happen in real life, RPIs must always be negative
        "when processing a single positive RPI it appear with a warning" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(
              EitherT.pure[Future, ErrorResponse](singlePositiveRPI)
            )

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(
              HistoricPayments(
                year = year,
                payments = Seq.empty
              )
            )
          }
        }

        "when fully open returns (nothing returned)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleFullyOutstandingReturn))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(HistoricPayments(year, Seq.empty))
          }
        }

        "when refunded (mustn't show)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singleRefundedReturn))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq.empty
              )
            )
          }
        }

        "when partial open return (the paid part)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singlePartiallyOutstandingReturn(onlyOpenItems = false)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            val chargeReference = historicPayments.toOption.get.payments.headOption.flatMap(_.chargeReference)
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq(
                  HistoricPayment(
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey),
                    TransactionType.Return,
                    chargeReference,
                    BigDecimal("4000")
                  )
                )
              )
            )
          }
        }

        "when fully paid (the original amount)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](singlePaidReturn))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            val chargeReference = historicPayments.toOption.get.payments.headOption.flatMap(_.chargeReference)
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq(
                  HistoricPayment(
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey),
                    TransactionType.Return,
                    chargeReference,
                    BigDecimal("9000")
                  )
                )
              )
            )
          }
        }

        "filter out nil returns where amounts offset to 0" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](nilReturnLineItemsCancelling))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq.empty
              )
            )
          }
        }

        "filter out fully open returns leaving the remaining ones" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](twoSeparateReturnsOneFullyPaid))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            val chargeReference = historicPayments.toOption.get.payments.headOption.flatMap(_.chargeReference)

            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq(
                  HistoricPayment(
                    ReturnPeriod.fromPeriodKeyOrThrow("24AE"),
                    TransactionType.Return,
                    chargeReference,
                    BigDecimal("9000")
                  )
                )
              )
            )
          }
        }

        "for multiple statuses" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year))
            .thenReturn(EitherT.pure[Future, ErrorResponse](multipleStatuses(onlyOpenItems = false)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            case Right(HistoricPayments(`year`, payments)) =>
              payments.map(payment =>
                payment.copy(chargeReference = payment.chargeReference.map(_ => "ChargeRef"))
              ) must contain theSameElementsAs Seq(
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AD"),
                  TransactionType.Return,
                  Some("ChargeRef"),
                  BigDecimal("2000")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AB"),
                  TransactionType.LPI,
                  Some("ChargeRef"),
                  BigDecimal("10")
                )
              )
            case _                                         => fail()
          }
        }
      }
    }
  }

  "when calling calculateTotalAmountPaid" - {
    "and there is no clearedAmount and it's not an RPI" - {
      "it must count it as 0 (coverage)" in new SetUp {
        paymentsService.calculateTotalAmountPaid(singleFullyOutstandingReturn.financialTransactions) mustBe BigDecimal(
          "0"
        )
      }
    }
  }

  class SetUp {
    val mockFinancialDataConnector                = mock[FinancialDataConnector]
    val paymentsValidator: PaymentsValidator      = new PaymentsValidator()
    val paymentsService                           = new HistoricPaymentsService(mockFinancialDataConnector, paymentsValidator)
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

    // Test edge case which mustn't happen as overpayments must reduce original amount
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
            maybeChargeReference = Some(chargeReferenceGen.sample.get)
          ),
          createFinancialDocument(
            onlyOpenItems = true,
            sapDocumentNumber = sapDocumentNumberGen.sample.get,
            originalAmount = BigDecimal("-2000"),
            maybeOutstandingAmount = Some(BigDecimal("-2000")),
            dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
            transactionType = TransactionType.Overpayment,
            maybePeriodKey = Some(periodKey),
            maybeChargeReference = None
          )
        )
      )

    // mustn't happen, RPI must be negative
    val singlePositiveRPI: FinancialTransactionDocument = {
      val sapDocumentNumber = sapDocumentNumberGen.sample.get
      val chargeReference   = chargeReferenceGen.sample.get

      createFinancialDocument(
        onlyOpenItems = false,
        sapDocumentNumber = sapDocumentNumber,
        originalAmount = BigDecimal("50"),
        maybeOutstandingAmount = Some(BigDecimal("50")),
        dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
        transactionType = TransactionType.RPI,
        maybePeriodKey = Some(periodKey),
        maybeChargeReference = Some(chargeReference)
      )
    }
  }
}

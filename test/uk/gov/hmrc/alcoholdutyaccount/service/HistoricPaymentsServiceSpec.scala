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

import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.ReturnPeriod
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument
import uk.gov.hmrc.alcoholdutyaccount.models.payments._
import uk.gov.hmrc.alcoholdutyaccount.utils.payments.PaymentsValidator

import scala.concurrent.Future

class HistoricPaymentsServiceSpec extends SpecBase {
  "HistoricPaymentsService" - {
    "when calling getHistoricPayments" - {
      "a successful and correct response must be returned" - {
        "handle no financial data (nil return or no data for the period)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(emptyFinancialDocument)))

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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(twoSeparateOverpayments(false))))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(HistoricPayments(year, Seq.empty))
          }
        }

        "when just RPI which must be filtered as it's a negative amount" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(singleRPI)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(
              HistoricPayments(
                year,
                Seq.empty
              )
            )
          }
        }

        "when processing a single positive RPI it appear with a warning" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(
              Future.successful(Right(singlePositiveRPI))
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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(singleFullyOutstandingReturn)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            _ mustBe Right(HistoricPayments(year, Seq.empty))
          }
        }

        "when refunded (mustn't show)" in new SetUp {
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(singleRefundedReturn)))

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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(singlePartiallyOutstandingReturn(onlyOpenItems = false))))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            val chargeReference = historicPayments.toOption.get.payments.headOption.flatMap(_.chargeReference)
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq(
                  HistoricPayment(
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey),
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate(),
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate(),
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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(singlePaidReturn)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            val chargeReference = historicPayments.toOption.get.payments.headOption.flatMap(_.chargeReference)
            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq(
                  HistoricPayment(
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey),
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate(),
                    ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate(),
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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(nilReturnLineItemsCancelling)))

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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(twoSeparateReturnsOneFullyPaid)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) { historicPayments =>
            val chargeReference = historicPayments.toOption.get.payments.headOption.flatMap(_.chargeReference)

            historicPayments mustBe Right(
              HistoricPayments(
                year,
                Seq(
                  HistoricPayment(
                    ReturnPeriod.fromPeriodKeyOrThrow("24AE"),
                    ReturnPeriod.fromPeriodKeyOrThrow("24AE").periodFromDate(),
                    ReturnPeriod.fromPeriodKeyOrThrow("24AE").periodToDate(),
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
          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(multipleStatuses(onlyOpenItems = false))))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value) {
            case Right(HistoricPayments(`year`, payments)) =>
              payments.map(payment =>
                payment.copy(chargeReference = payment.chargeReference.map(_ => "ChargeRef"))
              ) must contain theSameElementsAs Seq(
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AD"),
                  ReturnPeriod.fromPeriodKeyOrThrow("24AD").periodFromDate(),
                  ReturnPeriod.fromPeriodKeyOrThrow("24AD").periodToDate(),
                  TransactionType.Return,
                  Some("ChargeRef"),
                  BigDecimal("2000")
                ),
                HistoricPayment(
                  ReturnPeriod.fromPeriodKeyOrThrow("24AB"),
                  ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodFromDate(),
                  ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodToDate(),
                  TransactionType.LPI,
                  Some("ChargeRef"),
                  BigDecimal("10")
                )
              )
            case _                                         => fail()
          }
        }
      }

      "an exception must be thrown" - {
        "when taxPeriodFrom is absent for a historic payment to be cached" in new SetUp {
          val financialTransactionDocument = FinancialTransactionDocument(
            Seq(singlePaidReturn.financialTransactions.head.copy(taxPeriodFrom = None))
          )

          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(financialTransactionDocument)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value.failed) { ex =>
            ex            mustBe an[IllegalStateException]
            ex.getMessage mustBe "taxPeriodFrom is required for historic payments (Return/LPI/CA)"
          }
        }

        "when taxPeriodTo is absent for a historic payment to be cached" in new SetUp {
          val financialTransactionDocument = FinancialTransactionDocument(
            Seq(singlePaidReturn.financialTransactions.head.copy(taxPeriodTo = None))
          )

          when(mockFinancialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
            .thenReturn(Future.successful(Right(financialTransactionDocument)))

          whenReady(paymentsService.getHistoricPayments(appaId, year).value.failed) { ex =>
            ex            mustBe an[IllegalStateException]
            ex.getMessage mustBe "taxPeriodTo is required for historic payments (Return/LPI/CA)"
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
    val mockFinancialDataConnector: FinancialDataConnector = mock[FinancialDataConnector]
    val paymentsValidator: PaymentsValidator               = new PaymentsValidator()
    val paymentsService                                    = new HistoricPaymentsService(mockFinancialDataConnector, paymentsValidator)
    val year                                               = 2024

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
        maybeChargeReference = Some(chargeReference)
      )
    }
  }
}

/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.implicits._
import play.api.Logging
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.ReturnPeriod
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionDocument}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.TransactionType.RPI
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{HistoricPayment, HistoricPayments, TransactionType}
import uk.gov.hmrc.alcoholdutyaccount.utils.payments.PaymentsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.YearMonth
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HistoricPaymentsService @Inject() (
  financialDataConnector: FinancialDataConnector,
  financialDataValidator: PaymentsValidator
)(implicit ec: ExecutionContext)
    extends Logging {

  def getHistoricPayments(
    appaId: String,
    year: Int
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, HistoricPayments] =
    for {
      financialTransactionDocument <- EitherT(financialDataConnector.getNotOnlyOpenFinancialData(appaId, year))
      historicPayments             <- extractHistoricPayments(financialTransactionDocument)
    } yield HistoricPayments(year, historicPayments)

  private def extractHistoricPayments(
    financialTransactionDocument: FinancialTransactionDocument
  ): EitherT[Future, ErrorResponse, List[HistoricPayment]] =
    EitherT.fromEither(
      financialTransactionDocument.financialTransactions
        .filter(transaction =>
          TransactionType.isRPI(transaction.mainTransaction) ||
            !(TransactionType.isOverpayment(transaction.mainTransaction) || isTransactionFullyOpen(transaction))
        )
        .groupBy(_.sapDocumentNumber)
        .map { case (sapDocumentNumber, financialTransactionsForDocument) =>
          financialDataValidator
            .validateAndGetFinancialTransactionData(
              sapDocumentNumber,
              financialTransactionsForDocument
            )
            .map { financialTransactionData =>
              val totalAmountPaid = calculateTotalAmountPaid(financialTransactionsForDocument)

              if (totalAmountPaid > 0 && financialTransactionData.transactionType != RPI) {
                val taxPeriodFrom = financialTransactionData.taxPeriodFrom.getOrElse(
                  throw new IllegalStateException("taxPeriodFrom is required for historic payments (Return/LPI/CA)")
                )
                val taxPeriodTo   = financialTransactionData.taxPeriodTo.getOrElse(
                  throw new IllegalStateException("taxPeriodTo is required for historic payments (Return/LPI/CA)")
                )
                Some(
                  HistoricPayment(
                    taxPeriodFrom = taxPeriodFrom,
                    taxPeriodTo = taxPeriodTo,
                    period = financialTransactionData.maybePeriodKey
                      .flatMap(ReturnPeriod.fromPeriodKey)
                      .getOrElse(ReturnPeriod(YearMonth.from(taxPeriodTo))),
                    transactionType = financialTransactionData.transactionType,
                    chargeReference = financialTransactionData.maybeChargeReference,
                    amountPaid = totalAmountPaid
                  )
                )
              } else {
                None
              }
            }
        }
        .toList
        .sequence
        .map(_.flatten)
    )

  private[service] def calculateTotalAmountPaid(
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): BigDecimal =
    financialTransactionsForDocument
      .map(transaction =>
        if (TransactionType.isRPI(transaction.mainTransaction)) {
          transaction.originalAmount
        } else {
          transaction.clearedAmount.getOrElse(BigDecimal(0))
        }
      )
      .sum

  private def isTransactionFullyOpen(financialTransaction: FinancialTransaction): Boolean =
    financialTransaction.clearedAmount.isEmpty
}

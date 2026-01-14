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
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionDocument}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.TransactionType.Overpayment
import uk.gov.hmrc.alcoholdutyaccount.models.payments._
import uk.gov.hmrc.alcoholdutyaccount.utils.payments.PaymentsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OpenPaymentsService @Inject() (
  financialDataConnector: FinancialDataConnector,
  financialDataValidator: PaymentsValidator
)(implicit ec: ExecutionContext)
    extends Logging {

  private val contractObjectType = "ZADP"

  def getOpenPayments(
    appaId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, OpenPayments] =
    for {
      financialTransactionDocument <- EitherT(financialDataConnector.getOnlyOpenFinancialData(appaId))
      openPayments                 <- extractPayments(filterNonOverpayment(financialTransactionDocument))
      openOverPayments             <- extractOverPayments(filterOverpayment(financialTransactionDocument))
    } yield buildOpenPaymentsPayload(openPayments ::: openOverPayments)

  private def filterNonOverpayment(
    financialTransactionDocument: FinancialTransactionDocument
  ): Seq[FinancialTransaction] =
    financialTransactionDocument.financialTransactions
      .filter(transaction =>
        !TransactionType.isOverpayment(
          transaction.mainTransaction
        ) &&
          transaction.outstandingAmount.isDefined
      )

  private def filterOverpayment(financialTransactionDocument: FinancialTransactionDocument): Seq[FinancialTransaction] =
    financialTransactionDocument.financialTransactions
      .filter(transaction =>
        TransactionType.isOverpayment(
          transaction.mainTransaction
        ) &&
          transaction.contractObjectType.contains(contractObjectType) &&
          transaction.clearedAmount.isEmpty
      )

  private def extractPayments(
    transactions: Seq[FinancialTransaction]
  ): EitherT[Future, ErrorResponse, List[OpenPayment]] =
    EitherT {
      Future.successful(
        transactions
          .groupBy(_.sapDocumentNumber)
          .map { case (sapDocumentNumber, financialTransactionsForDocument) =>
            validate(sapDocumentNumber, financialTransactionsForDocument)
          }
          .toList
          .sequence
      )
    }

  private def extractOverPayments(
    transactions: Seq[FinancialTransaction]
  ): EitherT[Future, ErrorResponse, List[OpenPayment]] =
    EitherT {
      Future.successful(
        transactions
          .groupBy(x => (x.sapDocumentNumber, x.items.map(i => i.dueDate)))
          .map { case ((sapDocumentNumber, _), financialTransactionsForDocument) =>
            validate(sapDocumentNumber, financialTransactionsForDocument)
          }
          .toList
          .sequence
      )
    }

  private def validate(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Either[ErrorResponse, OpenPayment] =
    financialDataValidator
      .validateAndGetFinancialTransactionData(
        sapDocumentNumber,
        financialTransactionsForDocument
      )
      .map {
        val outstandingAmount = calculateOutstandingAmount(financialTransactionsForDocument)
        buildOpenPayment(_, outstandingAmount)
      }

  private def buildOpenPaymentsPayload(openPayments: List[OpenPayment]): OpenPayments = {
    val (outstandingPayments, unallocatedPayments) =
      openPayments.foldLeft((List.empty[OutstandingPayment], List.empty[UnallocatedPayment])) {
        case ((outstandingPayments, unallocatedPayments), openPayment) =>
          openPayment match {
            case outstandingPayment @ OutstandingPayment(_, _, _, _, _, _) =>
              (outstandingPayment :: outstandingPayments, unallocatedPayments)
            case unallocatedPayment @ UnallocatedPayment(_, _)             =>
              (outstandingPayments, unallocatedPayment :: unallocatedPayments)
          }
      }

    val paymentTotals = calculateTotalBalance(outstandingPayments, unallocatedPayments)

    OpenPayments(
      outstandingPayments = outstandingPayments,
      totalOutstandingPayments = paymentTotals.totalOutstandingPayments,
      unallocatedPayments = unallocatedPayments.sortBy(_.paymentDate),
      totalUnallocatedPayments = paymentTotals.totalUnallocatedPayments,
      totalOpenPaymentsAmount = paymentTotals.totalOpenPaymentsAmount
    )
  }

  private def calculateTotalBalance(
    outstandingPayments: Seq[OutstandingPayment],
    unallocatedPayments: Seq[UnallocatedPayment]
  ): PaymentTotals = {
    val totalOutstandingPayments = outstandingPayments.map(_.remainingAmount).sum
    val totalUnallocatedPayments = unallocatedPayments.map(_.unallocatedAmount).sum

    PaymentTotals(
      totalOutstandingPayments = totalOutstandingPayments,
      totalUnallocatedPayments = totalUnallocatedPayments,
      totalOpenPaymentsAmount = totalOutstandingPayments + totalUnallocatedPayments
    )
  }

  private[service] def calculateOutstandingAmount(
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): BigDecimal =
    financialTransactionsForDocument.map(_.outstandingAmount.getOrElse(BigDecimal(0))).sum

  private def buildOpenPayment(
    financialTransactionData: FinancialTransactionData,
    outstandingAmount: BigDecimal
  ): OpenPayment = {
    val transactionType = financialTransactionData.transactionType

    if (transactionType == Overpayment) {
      UnallocatedPayment(
        paymentDate = financialTransactionData.dueDate,
        unallocatedAmount = outstandingAmount
      )
    } else {
      OutstandingPayment(
        taxPeriodFrom = financialTransactionData.taxPeriodFrom,
        taxPeriodTo = financialTransactionData.taxPeriodTo,
        transactionType = transactionType,
        dueDate = financialTransactionData.dueDate,
        chargeReference = financialTransactionData.maybeChargeReference,
        remainingAmount = outstandingAmount
      )
    }
  }

}

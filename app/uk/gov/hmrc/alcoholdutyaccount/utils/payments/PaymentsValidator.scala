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

package uk.gov.hmrc.alcoholdutyaccount.utils.payments

import cats.implicits._
import play.api.Logging
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionItem}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.TransactionType.{Overpayment, RPI}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{FinancialTransactionData, TransactionType}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import javax.inject.Inject

class PaymentsValidator @Inject() () extends Logging {

  def validateAndGetFinancialTransactionData(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction],
    onlyOpenItems: Boolean
  ): Either[ErrorResponse, FinancialTransactionData] =
    // Various data has to be consistent across line items, so we need the first to obtain the data to compare against the rest
    for {
      firstFinancialTransactionLineItem <-
        getFirstFinancialTransactionLineItem(sapDocumentNumber, financialTransactionsForDocument)
      mainTransactionType                = firstFinancialTransactionLineItem.mainTransaction
      maybePeriodKey                     = firstFinancialTransactionLineItem.periodKey
      maybeChargeReference               = firstFinancialTransactionLineItem.chargeReference
      dueDate                           <- getFirstItemDueDate(sapDocumentNumber, firstFinancialTransactionLineItem)
      _                                 <- validateFinancialLineItems(
                                             sapDocumentNumber,
                                             financialTransactionsForDocument,
                                             mainTransactionType,
                                             maybePeriodKey,
                                             maybeChargeReference,
                                             dueDate
                                           )
      transactionType                   <- getTransactionTypeAndWarnIfOpenRPI(sapDocumentNumber, mainTransactionType, onlyOpenItems)
      _                                  =
        warnIfOverpaymentAndAmountsDontMatch(sapDocumentNumber, transactionType, financialTransactionsForDocument)
      _                                  = warnIfRPIAndIsPositive(sapDocumentNumber, transactionType, financialTransactionsForDocument)
    } yield FinancialTransactionData(
      transactionType,
      maybePeriodKey,
      dueDate,
      maybeChargeReference
    )

  private def getTransactionTypeAndWarnIfOpenRPI(
    sapDocumentNumber: String,
    mainTransactionType: String,
    onlyOpenItems: Boolean
  ): Either[ErrorResponse, TransactionType] =
    TransactionType
      .fromMainTransactionType(mainTransactionType)
      .fold[Either[ErrorResponse, TransactionType]] {
        logger.warn(
          s"Unexpected transaction type $mainTransactionType on financial transaction $sapDocumentNumber."
        )
        Left(ErrorCodes.unexpectedResponse)
      } { transactionType =>
        if (transactionType == RPI && onlyOpenItems) {
          logger.warn(
            s"Unexpected RPI in open payments on financial transaction $sapDocumentNumber."
          )
        }

        Right(transactionType)
      }

  private def warnIfOverpaymentAndAmountsDontMatch(
    sapDocumentNumber: String,
    transactionType: TransactionType,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Unit =
    if (
      transactionType == Overpayment &&
      financialTransactionsForDocument.exists(financialTransactionLineItem =>
        !financialTransactionLineItem.outstandingAmount.contains(financialTransactionLineItem.originalAmount)
      )
    ) {
      logger.warn(
        s"Expected original and outstanding amounts to match on overpayment on financial transaction $sapDocumentNumber."
      )
    }

  private def warnIfRPIAndIsPositive(
    sapDocumentNumber: String,
    transactionType: TransactionType,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Unit =
    if (
      transactionType == RPI &&
      financialTransactionsForDocument.exists(financialTransactionLineItem =>
        financialTransactionLineItem.outstandingAmount.exists(_ > 0) || financialTransactionLineItem.originalAmount > 0
      )
    ) {
      logger.warn(
        s"Expected original and outstanding amounts of an RPI to be non-positive on financial transaction $sapDocumentNumber."
      )
    }

  private def validateFinancialLineItems(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction],
    mainTransactionType: String,
    maybePeriodKey: Option[String],
    maybeChargeReference: Option[String],
    dueDate: LocalDate
  ): Either[ErrorResponse, Unit] =
    // Will need to identity check most fields on the first lines item so that due dates are also checked on all its items
    financialTransactionsForDocument
      .map(financialTransaction =>
        if (financialTransaction.items.isEmpty) {
          logger.warn(s"Expected at least one item on line items for financial transaction $sapDocumentNumber.")
          Left(ErrorCodes.unexpectedResponse)
        } else {
          Right(
            mainTransactionType == financialTransaction.mainTransaction &&
              maybePeriodKey == financialTransaction.periodKey &&
              maybeChargeReference == financialTransaction.chargeReference &&
              financialTransaction.items.forall(_.dueDate.fold(false)(_.isEqual(dueDate)))
          )
        }
      )
      .toList
      .sequence // Right is a list of whether all validation checks passed (true)
      .filterOrElse(
        _.forall(identity), {
          logger.warn(
            s"Not all chargeReferences, periodKeys, mainTransactions and/or dueDates matched against the first entry of financial transaction $sapDocumentNumber."
          )
          ErrorCodes.unexpectedResponse
        }
      )
      .map(_ => ())

  private def getFirstItemDueDate(
    sapDocumentNumber: String,
    financialTransactionLineItem: FinancialTransaction
  ): Either[ErrorResponse, LocalDate] =
    financialTransactionLineItem.items.toList match {
      case FinancialTransactionItem(_, Some(dueDate), _) :: _ => Right(dueDate)
      case FinancialTransactionItem(_, None, _) :: _          =>
        logger.warn(s"Due date not found on first item of first entry of financial transaction $sapDocumentNumber.")
        Left(ErrorCodes.unexpectedResponse)
      case _                                                  =>
        logger.warn(s"Expected at least one item on line items for financial transaction $sapDocumentNumber.")
        Left(ErrorCodes.unexpectedResponse)
    }

  private def getFirstFinancialTransactionLineItem(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Either[ErrorResponse, FinancialTransaction] =
    financialTransactionsForDocument.toList match {
      case firstFinancialTransactionLineItem :: _ => Right(firstFinancialTransactionLineItem)
      case _                                      =>
        logger.warn(
          s"Should have had a least one entry for financial transaction $sapDocumentNumber. This shouldn't happen"
        )

        Left(ErrorCodes.unexpectedResponse)
    }
}

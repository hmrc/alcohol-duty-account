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
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{FinancialTransactionData, TransactionType}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import javax.inject.Inject

class PaymentsValidator @Inject() () extends Logging {

  def validateAndGetFinancialTransactionData(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Either[ErrorResponse, FinancialTransactionData] =
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
      transactionType                   <- getTransactionType(sapDocumentNumber, mainTransactionType)
    } yield FinancialTransactionData(
      transactionType,
      maybePeriodKey,
      dueDate,
      maybeChargeReference
    )

  private def getTransactionType(
    sapDocumentNumber: String,
    mainTransactionType: String
  ): Either[ErrorResponse, TransactionType] =
    TransactionType
      .fromMainTransactionType(mainTransactionType)
      .fold[Either[ErrorResponse, TransactionType]] {
        logger.warn(
          s"Unexpected transaction type $mainTransactionType on financial transaction $sapDocumentNumber."
        )
        Left(ErrorCodes.unexpectedResponse)
      } { transactionType =>
        Right(transactionType)
      }

  private def validateFinancialLineItems(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction],
    mainTransactionType: String,
    maybePeriodKey: Option[String],
    maybeChargeReference: Option[String],
    dueDate: LocalDate
  ): Either[ErrorResponse, Unit] =
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
      .sequence
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

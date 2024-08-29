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
import cats.implicits._
import play.api.Logging
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionDocument, FinancialTransactionItem}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.TransactionType.{PaymentOnAccount, RPI}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{OpenPayment, OpenPayments, OutstandingPayment, TransactionType, UnallocatedPayment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsService @Inject() (
  financialDataConnector: FinancialDataConnector
)(implicit ec: ExecutionContext)
    extends Logging {
  private[service] case class FinancialTransactionData(
    transactionType: TransactionType,
    dueDate: LocalDate,
    maybeChargeReference: Option[String]
  )

  private case class PaymentTotals(
    totalOutstandingPayments: BigDecimal,
    totalUnallocatedPayments: BigDecimal,
    totalOpenPaymentsAmount: BigDecimal
  )

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

  private def validateFinancialLineItems(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction],
    mainTransactionType: String,
    chargeReference: Option[String],
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
              chargeReference == financialTransaction.chargeReference &&
              financialTransaction.items.forall(_.dueDate.fold(false)(_.isEqual(dueDate)))
          )
        }
      )
      .toList
      .sequence // Right is a list of whether all validation checks passed (true)
      .filterOrElse(
        _.forall(identity), {
          logger.warn(
            s"Not all chargeReferences, mainTransactions and/or dueDates matched against the first entry of financial transaction $sapDocumentNumber."
          )
          ErrorCodes.unexpectedResponse
        }
      )
      .map(_ => ())

  private def getTransactionTypeAndWarnIfOpenRPI(
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
        if (transactionType == RPI) {
          logger.warn(
            s"Unexpected RPI in open payments on financial transaction $sapDocumentNumber."
          )
        }

        Right(transactionType)
      }

  private def warnIfPaymentOnAccountAmountsDontMatch(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Unit =
    if (
      financialTransactionsForDocument.exists(financialTransactionLineItem =>
        financialTransactionLineItem.outstandingAmount.contains(financialTransactionLineItem.originalAmount)
      )
    ) {
      logger.warn(
        s"Expected original and outstanding amounts to match on payment on account on financial transaction $sapDocumentNumber."
      )
    }

  private[service] def validateAndGetFinancialTransactionData(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Either[ErrorResponse, FinancialTransactionData] =
    // Various data has to be consistent across line items, so we need the first to obtain the data to compare against the rest
    for {
      firstFinancialTransactionLineItem <-
        getFirstFinancialTransactionLineItem(sapDocumentNumber, financialTransactionsForDocument)
      mainTransactionType                = firstFinancialTransactionLineItem.mainTransaction
      maybeChargeReference               = firstFinancialTransactionLineItem.chargeReference
      dueDate                           <- getFirstItemDueDate(sapDocumentNumber, firstFinancialTransactionLineItem)
      _                                 <- validateFinancialLineItems(
                                             sapDocumentNumber,
                                             financialTransactionsForDocument,
                                             mainTransactionType,
                                             maybeChargeReference,
                                             dueDate
                                           )
      transactionType                   <- getTransactionTypeAndWarnIfOpenRPI(sapDocumentNumber, mainTransactionType)
      _                                  = warnIfPaymentOnAccountAmountsDontMatch(sapDocumentNumber, financialTransactionsForDocument)
    } yield FinancialTransactionData(
      transactionType,
      dueDate,
      maybeChargeReference
    )

  private def calculatedTotalBalance(
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

    if (transactionType == PaymentOnAccount) {
      UnallocatedPayment(
        paymentDate = financialTransactionData.dueDate,
        unallocatedAmount = outstandingAmount
      )
    } else {
      OutstandingPayment(
        transactionType = transactionType,
        dueDate = financialTransactionData.dueDate,
        chargeReference = financialTransactionData.maybeChargeReference,
        remainingAmount = outstandingAmount
      )
    }
  }

  private def extractOpenPayments(
    financialTransactionDocument: FinancialTransactionDocument
  ): EitherT[Future, ErrorResponse, List[OpenPayment]] =
    EitherT {
      Future.successful(
        financialTransactionDocument.financialTransactions
          .groupBy(_.sapDocumentNumber)
          .map { case (sapDocumentNumber, financialTransactionsForDocument) =>
            validateAndGetFinancialTransactionData(sapDocumentNumber, financialTransactionsForDocument)
              .map {
                val outstandingAmount = calculateOutstandingAmount(financialTransactionsForDocument)
                buildOpenPayment(_, outstandingAmount)
              }
          }
          .toList
          .sequence
      )
    }

  private def buildOpenPaymentsPayload(openPayments: List[OpenPayment]): OpenPayments = {
    val (outstandingPayments, unallocatedPayments) =
      openPayments.foldLeft((List.empty[OutstandingPayment], List.empty[UnallocatedPayment])) {
        case ((outstandingPayments, unallocatedPayments), openPayment) =>
          openPayment match {
            case outstandingPayment @ OutstandingPayment(_, _, _, _) =>
              (outstandingPayment :: outstandingPayments, unallocatedPayments)
            case unallocatedPayment @ UnallocatedPayment(_, _)       =>
              (outstandingPayments, unallocatedPayment :: unallocatedPayments)
          }
      }

    val paymentTotals = calculatedTotalBalance(outstandingPayments, unallocatedPayments)

    OpenPayments(
      outstandingPayments = outstandingPayments,
      totalOutstandingPayments = paymentTotals.totalOutstandingPayments,
      unallocatedPayments = unallocatedPayments,
      totalUnallocatedPayments = paymentTotals.totalUnallocatedPayments,
      totalOpenPaymentsAmount = paymentTotals.totalOpenPaymentsAmount
    )
  }

  def getOpenPayments(
    appaId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, OpenPayments] =
    for {
      financialTransactionDocument <- financialDataConnector.getFinancialData(appaId)
      openPayments                 <- extractOpenPayments(financialTransactionDocument)
    } yield buildOpenPaymentsPayload(openPayments)
}

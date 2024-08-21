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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransaction, FinancialTransactionDocument}
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

  private def validateAndGetDueDateFromFirstTransaction(
    sapDocumentNumber: String,
    firstFinancialTransaction: FinancialTransaction
  ): Either[ErrorResponse, LocalDate] = firstFinancialTransaction.items.toList match {
    case firstItem :: subsequentItems =>
      firstItem.dueDate.fold[Either[ErrorResponse, LocalDate]] {
        val errorMessage =
          s"Due date not found on first item of first entry of financial transaction $sapDocumentNumber."
        logger.warn(errorMessage)
        Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorMessage))
      } { dueDate =>
        val hasErrors = subsequentItems.map(_.dueDate.fold(true)(!dueDate.isEqual(_))).exists(identity)

        if (hasErrors) {
          val errorMessage = s"Not all dueDates matched for first entry of financial transaction $sapDocumentNumber."
          logger.warn(errorMessage)
          Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorMessage))
        } else {
          Right(dueDate)
        }
      }
    case _                            =>
      val errorMessage = s"Expected at least one item for financial transaction $sapDocumentNumber."
      logger.warn(errorMessage)
      Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorMessage))
  }

  private def validateAndGetFinancialTransactionData(
    sapDocumentNumber: String,
    dueDate: LocalDate,
    firstFinancialTransaction: FinancialTransaction,
    subsequentFinancialTransactions: List[FinancialTransaction]
  ): Either[ErrorResponse, FinancialTransactionData] = {
    val mainTransactionType = firstFinancialTransaction.mainTransaction
    val chargeReference     = firstFinancialTransaction.chargeReference

    val hasErrors = subsequentFinancialTransactions
      .map(nextFinancialTransaction =>
        !(mainTransactionType == nextFinancialTransaction.mainTransaction &&
          chargeReference == nextFinancialTransaction.chargeReference &&
          nextFinancialTransaction.items.forall(_.dueDate.fold(false)(_.isEqual(dueDate))))
      )
      .exists(identity)

    if (hasErrors) {
      val errorMessage =
        s"Not all chargeReferences, mainTransactions and/or dueDates matched against the first entry of financial transaction $sapDocumentNumber."
      logger.warn(errorMessage)
      Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorMessage))
    } else {
      TransactionType
        .fromMainTransactionType(mainTransactionType)
        .fold[Either[ErrorResponse, FinancialTransactionData]] {
          val errorMessage =
            s"Unexpected transaction type $mainTransactionType on financial transaction $sapDocumentNumber."
          logger.warn(errorMessage)
          Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorMessage))
        } { transactionType =>
          if (transactionType == RPI) {
            val errorMessage =
              s"Unexpected RPI in open payments on financial transaction $sapDocumentNumber."
            logger.warn(errorMessage)
          }

          Right(
            FinancialTransactionData(
              transactionType,
              dueDate,
              chargeReference
            )
          )
        }
    }
  }

  private[service] def validateAndGetCommonData(
    sapDocumentNumber: String,
    financialTransactionsForDocument: Seq[FinancialTransaction]
  ): Either[ErrorResponse, FinancialTransactionData] = financialTransactionsForDocument.toList match {
    case firstFinancialTransaction :: subsequentFinancialTransactions =>
      for {
        dueDate                  <- validateAndGetDueDateFromFirstTransaction(sapDocumentNumber, firstFinancialTransaction)
        financialTransactionData <- validateAndGetFinancialTransactionData(
                                      sapDocumentNumber,
                                      dueDate,
                                      firstFinancialTransaction,
                                      subsequentFinancialTransactions
                                    )
      } yield financialTransactionData

    case _ =>
      val errorMessage =
        s"Should have had a least one entry for financial transaction $sapDocumentNumber. This shouldn't happen"
      logger.warn(errorMessage)
      Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorMessage))
  }

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
            validateAndGetCommonData(sapDocumentNumber, financialTransactionsForDocument)
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

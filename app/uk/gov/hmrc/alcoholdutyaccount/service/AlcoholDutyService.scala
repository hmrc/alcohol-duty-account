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

import cats.data.{EitherT, OptionT}
import cats.implicits._
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.alcoholdutyaccount.connectors.{FinancialDataConnector, ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ObligationStatus, _}
import uk.gov.hmrc.alcoholdutyaccount.models.payments.TransactionType
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.ApprovalStatus.{DeRegistered, Revoked, SmallCiderProducer}
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.{AdrSubscriptionSummary, ApprovalStatus}
import uk.gov.hmrc.alcoholdutyaccount.utils.DateTimeHelper.instantToLocalDate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AlcoholDutyService @Inject() (
  subscriptionSummaryConnector: SubscriptionSummaryConnector,
  obligationDataConnector: ObligationDataConnector,
  financialDataConnector: FinancialDataConnector,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends Logging {

  def getSubscriptionSummary(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AdrSubscriptionSummary] =
    EitherT {
      subscriptionSummaryConnector
        .getSubscriptionSummary(alcoholDutyReference)
        .map {
          case Left(error)    => Left(error)
          case Right(summary) => AdrSubscriptionSummary.fromSubscriptionSummary(summary)
        }
    }

  def getOpenObligations(
    alcoholDutyReference: String,
    periodKey: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AdrObligationData] =
    obligationDataConnector
      .getObligationDetails(alcoholDutyReference, Some(Open))
      .map(findObligationDetailsForPeriod(_, periodKey))
      .subflatMap {
        case None                    =>
          Left(ErrorResponse(NOT_FOUND, s"Obligation details not found for period key $periodKey"))
        case Some(obligationDetails) =>
          Right[ErrorResponse, AdrObligationData](AdrObligationData(obligationDetails))
      }

  def getObligations(
    alcoholDutyReference: String,
    obligationStatusFilter: Option[ObligationStatus]
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, Seq[AdrObligationData]] =
    obligationDataConnector
      .getObligationDetails(alcoholDutyReference, obligationStatusFilter)
      .map(_.obligations.flatMap(_.obligationDetails.map(AdrObligationData(_))))

  private def findObligationDetailsForPeriod(
    obligationData: ObligationData,
    periodKey: String
  ): Option[ObligationDetails] =
    obligationData.obligations.flatMap(_.obligationDetails).find(_.periodKey == periodKey)

  def getAlcoholDutyCardData(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AlcoholDutyCardData] =
    getSubscriptionSummary(alcoholDutyReference)
      .flatMapF { subscriptionSummary =>
        if (
          subscriptionSummary.approvalStatus == SmallCiderProducer ||
          subscriptionSummary.approvalStatus == DeRegistered ||
          subscriptionSummary.approvalStatus == Revoked
        ) {
          Future.successful(Right(RestrictedCardData(alcoholDutyReference, subscriptionSummary.approvalStatus)))
        } else {
          getObligationAndFinancialInfo(alcoholDutyReference, subscriptionSummary.approvalStatus)
        }
      }
      .recover { case errorResponse: ErrorResponse =>
        logger.warn(
          s"Failed to retrieve subscription summary, returning an error card with hasSubscriptionSummaryError flag set. Error: $errorResponse"
        )
        AlcoholDutyCardData(
          alcoholDutyReference = alcoholDutyReference,
          approvalStatus = None,
          hasSubscriptionSummaryError = true,
          hasReturnsError = false,
          hasPaymentsError = false,
          returns = Returns(),
          payments = Payments()
        )
      }

  private def getObligationAndFinancialInfo(alcoholDutyReference: String, approvalStatus: ApprovalStatus)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, AlcoholDutyCardData]] = {
    val obDataFuture = getReturnDetails(alcoholDutyReference)
    val fDataFuture  = getPaymentInformation(alcoholDutyReference)
    for {
      obData <- obDataFuture
      fData  <- fDataFuture
    } yield AlcoholDutyCardData(
      alcoholDutyReference = alcoholDutyReference,
      approvalStatus = Some(approvalStatus),
      hasSubscriptionSummaryError = false,
      hasReturnsError = obData.isEmpty,
      hasPaymentsError = fData.isEmpty,
      returns = obData.getOrElse(Returns()),
      payments = fData.getOrElse(Payments())
    ).asRight
  }

  private[service] def getReturnDetails(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): Future[Option[Returns]] =
    obligationDataConnector
      .getObligationDetails(alcoholDutyReference, Some(Open))
      .fold(_ => None, obligationData => Some(extractReturns(obligationData.obligations.flatMap(_.obligationDetails))))

  private[service] def extractReturns(obligationDetails: Seq[ObligationDetails]): Returns =
    if (obligationDetails.isEmpty) {
      Returns()
    } else {
      val now = instantToLocalDate(Instant.now(clock))

      val dueReturnExists: Boolean    =
        obligationDetails.exists(_.inboundCorrespondenceDueDate.isAfter(now.minusDays(1)))
      val numberOfOverdueReturns: Int =
        obligationDetails.count(_.inboundCorrespondenceDueDate.isBefore(now))
      val periodKey                   = obligationDetails match {
        case Seq(singleObligation) => Some(singleObligation.periodKey)
        case _                     => None
      }

      Returns(Some(dueReturnExists), Some(numberOfOverdueReturns), periodKey)
    }

  /*
   * Wrapper to preserve call for BTA Tile API which wants an OptionT
   */
  private def getFinancialDataForBtaTile(
    appaId: String
  )(implicit hc: HeaderCarrier): OptionT[Future, FinancialTransactionDocument] = OptionT(
    financialDataConnector.getOnlyOpenFinancialData(appaId).value.map {
      case Left(ErrorResponse(NOT_FOUND, _, _, _)) => Some(FinancialTransactionDocument(Seq.empty))
      case Left(_)                                 => None
      case Right(result)                           => Some(result)
    }
  )

  private def removeNonZADPOverpayments(
    financialTransactions: Seq[FinancialTransaction]
  ): Seq[FinancialTransaction] =
    financialTransactions.filter(financialTransaction =>
      !TransactionType.isOverpayment(financialTransaction.mainTransaction) ||
        financialTransaction.contractObjectType.contains("ZADP")
    )

  private[service] def getPaymentInformation(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): Future[Option[Payments]] =
    getFinancialDataForBtaTile(alcoholDutyReference)
      .fold(Option.empty[Payments])(financialData =>
        Some(extractPayments(removeNonZADPOverpayments(financialData.financialTransactions)))
      )

  private[service] def extractPayments(financialTransactions: Seq[FinancialTransaction]): Payments = {
    val transactionsBySapDocumentNumber: Map[String, Seq[FinancialTransaction]] =
      financialTransactions.groupBy(_.sapDocumentNumber)
    val totalPaymentAmount: BigDecimal                                          = financialTransactions.flatMap(_.outstandingAmount).sum

    transactionsBySapDocumentNumber.toList match {
      case Nil                     => Payments()
      case List(_ -> transactions) =>
        val chargeReferences: Seq[Option[String]] = transactions.map(_.chargeReference).distinct

        /*
            If there is one group of transactions by sapDocumentNumber but those group of transactions don't have a charge reference or the same charge references,
            we still return isMultiplePaymentDue = true because we'd want the user to pay with appaid in that case (as there is no charge ref found for payment)
         */
        val (chargeReferenceToUse, isMultiplePaymentsDue) = chargeReferences match {
          case Seq(maybeChargeReference @ Some(_)) => (maybeChargeReference, false)
          case _                                   => (None, true)
        }

        Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = totalPaymentAmount,
              isMultiplePaymentDue = isMultiplePaymentsDue,
              chargeReference = chargeReferenceToUse
            )
          )
        )

      case _ =>
        Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = totalPaymentAmount,
              isMultiplePaymentDue = true,
              chargeReference = None
            )
          )
        )
    }
  }
}

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
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.alcoholdutyaccount.connectors.{FinancialDataConnector, ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus.{DeRegistered, Revoked, SmallCiderProducer}
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransactionDocument, ObligationData, ObligationDetails, ObligationStatus, Open, SubscriptionSummary}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AlcoholDutyService @Inject() (
  subscriptionSummaryConnector: SubscriptionSummaryConnector,
  obligationDataConnector: ObligationDataConnector,
  financialDataConnector: FinancialDataConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def getSubscriptionSummary(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AdrSubscriptionSummary] = EitherT {
    subscriptionSummaryConnector
      .getSubscriptionSummary(alcoholDutyReference)
      .foldF(
        errorResponse => Future.successful(Left(errorResponse)),
        subscriptionSummary => Future.successful(Right(AdrSubscriptionSummary(subscriptionSummary)))
      )
  }

  def getOpenObligations(
    alcoholDutyReference: String,
    periodKey: String,
    obligationStatusFilter: Option[ObligationStatus]
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AdrObligationData] =
    obligationDataConnector
      .getObligationDetails(alcoholDutyReference, obligationStatusFilter)
      .map(findObligationDetailsForPeriod(_, periodKey))
      .transform {
        case l @ Left(_)                    => l.asInstanceOf[Either[ErrorResponse, AdrObligationData]]
        case Right(None)                    =>
          Left(ErrorResponse(NOT_FOUND, s"Obligation details not found for period key $periodKey"))
        case Right(Some(obligationDetails)) =>
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
    subscriptionSummaryConnector
      .getSubscriptionSummary(alcoholDutyReference)
      .flatMapF { subscriptionSummary: SubscriptionSummary =>
        val approvalStatus = ApprovalStatus(subscriptionSummary)
        if (
          approvalStatus == SmallCiderProducer ||
          approvalStatus == DeRegistered ||
          approvalStatus == Revoked
        ) { Future.successful(Right(RestrictedCardData(alcoholDutyReference, approvalStatus))) }
        else { getObligationAndFinancialInfo(alcoholDutyReference, approvalStatus) }
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
          hasPaymentError = false,
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
      hasPaymentError = fData.isEmpty,
      returns = obData.getOrElse(Returns()),
      payments = fData.getOrElse(Payments())
    ).asRight
  }

  private[service] def getReturnDetails(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): Future[Option[Returns]] =
    obligationDataConnector
      .getObligationDetails(alcoholDutyReference, Some(Open))
      .toOption
      .fold {
        None: Option[Returns]
      } { obligationData =>
        Some(extractReturns(obligationData.obligations.flatMap(_.obligationDetails)))
      }

  private[service] def extractReturns(obligationDetails: Seq[ObligationDetails]): Returns =
    if (obligationDetails.isEmpty) {
      Returns()
    } else {
      val now = LocalDate.now()

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

  private[service] def getPaymentInformation(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): Future[Option[Payments]] =
    financialDataConnector
      .getFinancialData(alcoholDutyReference)
      .fold {
        Option.empty[Payments]
      } { financialData =>
        Some(extractPayments(financialData))
      }

  private[service] def extractPayments(financialDocument: FinancialTransactionDocument): Payments = {
    val payments = financialDocument.financialTransactions.groupBy(_.chargeReference)
    payments.size match {
      case 0 => Payments()
      case 1 =>
        val (chargingReference, transaction) = payments.head
        Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = transaction.map(_.outstandingAmount).sum,
              isMultiplePaymentDue = false,
              chargeReference = Some(chargingReference)
            )
          )
        )
      case _ =>
        Payments(
          balance = Some(
            Balance(
              totalPaymentAmount = financialDocument.financialTransactions.map(_.outstandingAmount).sum,
              isMultiplePaymentDue = true,
              chargeReference = None
            )
          )
        )
    }
  }
}

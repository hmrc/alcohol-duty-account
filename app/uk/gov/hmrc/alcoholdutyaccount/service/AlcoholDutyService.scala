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
import play.api.http.Status.{NOT_FOUND, NOT_IMPLEMENTED}
import uk.gov.hmrc.alcoholdutyaccount.connectors.{FinancialDataConnector, ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models.{AlcoholDutyCardData, Approved, Balance, InsolventCardData, Payments, ReturnPeriod, Returns, hods}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{FinancialTransactionDocument, ObligationData, ObligationDetails}
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
)(implicit ec: ExecutionContext) {
  def getObligations(
    alcoholDutyReference: String,
    returnPeriod: ReturnPeriod
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, ObligationDetails] =
    obligationDataConnector
      .getOpenObligationDetails(alcoholDutyReference)
      .map(findObligationDetailsForPeriod(_, returnPeriod))
      .transform {
        case l @ Left(_)                    => l.asInstanceOf[Either[ErrorResponse, ObligationDetails]]
        case Right(None)                    => Left(ErrorResponse(NOT_FOUND, ""))
        case Right(Some(obligationDetails)) => Right[ErrorResponse, ObligationDetails](obligationDetails)
      }

  private def findObligationDetailsForPeriod(
    obligationData: ObligationData,
    returnPeriod: ReturnPeriod
  ): Option[ObligationDetails] =
    obligationData.obligations.flatMap(_.obligationDetails).find(_.periodKey == returnPeriod.periodKey)

  def getAlcoholDutyCardData(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AlcoholDutyCardData] = EitherT {
    subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference).value.flatMap {
      case Some(subscriptionSummary) if subscriptionSummary.insolvencyFlag                  =>
        Future.successful(Right(InsolventCardData(alcoholDutyReference)))
      case Some(subscriptionSummary) if subscriptionSummary.approvalStatus == hods.Approved =>
        getObligationAndFinancialInfo(alcoholDutyReference)
      case Some(_)                                                                          =>
        Future.successful(Left(ErrorResponse(NOT_IMPLEMENTED, "Approval Status not yet supported")))
      case None                                                                             =>
        Future.successful(Left(ErrorResponse(NOT_FOUND, "Subscription Summary not found")))
    }
  }

  private def getObligationAndFinancialInfo(alcoholDutyReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, AlcoholDutyCardData]] = for {
    obData <- getReturnDetails(alcoholDutyReference)
    fData  <- getPaymentInformation(alcoholDutyReference)
  } yield AlcoholDutyCardData(
    alcoholDutyReference = alcoholDutyReference,
    approvalStatus = Approved,
    hasReturnsError = obData.isEmpty,
    hasPaymentError = fData.isEmpty,
    returns = obData.getOrElse(Returns()),
    payments = fData.getOrElse(Payments())
  ).asRight

  private[service] def getReturnDetails(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): Future[Option[Returns]] =
    obligationDataConnector
      .getOpenObligationDetails(alcoholDutyReference)
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

      val dueReturnExists        =
        obligationDetails.exists(_.inboundCorrespondenceDueDate.isAfter(now.minusDays(1)))
      val numberOfOverdueReturns =
        obligationDetails.count(_.inboundCorrespondenceDueDate.isBefore(now))
      Returns(Some(dueReturnExists), Some(numberOfOverdueReturns))
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

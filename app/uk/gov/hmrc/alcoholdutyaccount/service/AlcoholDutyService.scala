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
import play.api.http.Status.{NOT_FOUND, NOT_IMPLEMENTED}
import uk.gov.hmrc.alcoholdutyaccount.connectors.{FinancialDataConnector, ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models.{AlcoholDutyCardData, Approved, ErrorResponse, InsolventCardData, Payments, Returns, hods}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Document, ObligationDetails, Open, SubscriptionSummary}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AlcoholDutyService @Inject() (
  subscriptionSummaryConnector: SubscriptionSummaryConnector,
  obligationDataConnector: ObligationDataConnector,
  financialDataConnector: FinancialDataConnector
)(implicit ec: ExecutionContext) {

  def getAlcoholDutyCardData(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, AlcoholDutyCardData] = EitherT {
    subscriptionSummaryConnector.getSubscriptionSummary(alcoholDutyReference).value.flatMap {
      case None                      => Future.successful(Left(ErrorResponse(NOT_FOUND, "Subscription Summary not found")))
      case Some(subscriptionSummary) => getObligationAndFinancialInfo(alcoholDutyReference, subscriptionSummary).value
    }
  }
  private def getObligationAndFinancialInfo(alcoholDutyReference: String, subscriptionSummary: SubscriptionSummary)(
    implicit hc: HeaderCarrier
  ): EitherT[Future, ErrorResponse, AlcoholDutyCardData]                             =
    if (subscriptionSummary.insolvencyFlag) {
      EitherT.rightT(InsolventCardData(alcoholDutyReference))
    } else if (subscriptionSummary.approvalStatus == hods.Approved) {
      val result = for {
        obData <- getObligationData(alcoholDutyReference)
        fData  <- getPaymentInformation(alcoholDutyReference)
      } yield AlcoholDutyCardData(
        alcoholDutyReference = alcoholDutyReference,
        approvalStatus = Approved,
        hasReturnsError = obData.isEmpty,
        hasPaymentError = fData.isEmpty,
        returns = obData.getOrElse(Returns()),
        payments = fData.getOrElse(Payments())
      )
      EitherT.right(result)
    } else {
      EitherT.leftT(ErrorResponse(NOT_IMPLEMENTED, "Approval Status not yet supported"))
    }

  def getObligationData(alcoholDutyReference: String)(implicit hc: HeaderCarrier): Future[Option[Returns]] =
    obligationDataConnector
      .getObligationData(alcoholDutyReference)
      .fold {
        None: Option[Returns]
      } { obligationData =>
        Some(extractReturns(obligationData.obligations.flatMap(_.obligationDetails)))
      }

  def extractReturns(obligationDetails: Seq[ObligationDetails]): Returns =
    if (obligationDetails.isEmpty) {
      Returns()
    } else {
      val dueReturnExists        =
        obligationDetails.exists { o =>
          o.status == Open && o.inboundCorrespondenceDueDate.isAfter(LocalDate.now().minusDays(1))
        }
      val numberOfOverdueReturns =
        obligationDetails.count(o => o.status == Open && o.inboundCorrespondenceDueDate.isBefore(LocalDate.now()))
      Returns(Some(dueReturnExists), Some(numberOfOverdueReturns))
    }

  def getPaymentInformation(
    alcoholDutyReference: String
  )(implicit hc: HeaderCarrier): Future[Option[Payments]] =
    financialDataConnector
      .getFinancialData(alcoholDutyReference)
      .fold {
        None: Option[Payments]
      } { financialData =>
        Some(extractPayments(financialData))
      }

  def extractPayments(financialDocument: Document): Payments = {
    val payments = financialDocument.financialTransactions.groupBy(_.chargeReference)
    payments.size match {
      case 0 => Payments()
      case 1 =>
        val (chargingReference, transaction) = payments.head
        Payments(
          totalPaymentAmount = Some(transaction.map(_.outstandingAmount).sum),
          isMultiplePaymentDue = Some(false),
          chargeReference = Some(chargingReference)
        )
      case _ =>
        Payments(
          totalPaymentAmount = Some(financialDocument.financialTransactions.map(_.outstandingAmount).sum),
          isMultiplePaymentDue = Some(true),
          chargeReference = None
        )
    }
  }
}

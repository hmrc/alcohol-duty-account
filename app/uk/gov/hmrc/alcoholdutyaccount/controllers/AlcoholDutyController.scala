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

package uk.gov.hmrc.alcoholdutyaccount.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.alcoholdutyaccount.connectors.{ObligationDataConnector, SubscriptionSummaryConnector}
import uk.gov.hmrc.alcoholdutyaccount.models._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Obligation, Open}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AlcoholDutyController @Inject() (
  subscriptionSummaryConnector: SubscriptionSummaryConnector,
  obligationDataConnector: ObligationDataConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def btaTileData(alcoholDutyReference: String): Action[AnyContent] = Action.async { implicit request =>
    subscriptionSummaryConnector
      .getSubscriptionSummary(alcoholDutyReference)
      .foldF {
        Future.successful(getError("No subscription summary found"))
      } { subscriptionSummary =>
        subscriptionSummary.approvalStatus match {
          case _ if subscriptionSummary.insolvencyFlag =>
            Future.successful(
              Ok(Json.toJson(AlcoholDutyCardData(alcoholDutyReference, Insolvent, false, Seq.empty)))
            )
          case hods.Approved                           =>
            getObligationData(alcoholDutyReference).map { obData =>
              val cardData = AlcoholDutyCardData(
                alcoholDutyReference = alcoholDutyReference,
                approvalStatus = Approved,
                hasReturnsError = obData.isEmpty,
                returns = obData.getOrElse(Seq.empty)
              )
              Ok(Json.toJson(cardData))
            }
          case _                                       => Future.successful(getError("Approval Status not yet supported"))
        }
      }
  }

  private def getObligationData(alcoholDutyReference: String)(implicit hc: HeaderCarrier): Future[Option[Seq[Return]]] =
    obligationDataConnector
      .getObligationData(alcoholDutyReference)
      .fold {
        None: Option[Seq[Return]]
      } { obligationData =>
        Some(obligationData.obligations.map(dueReturnExists))
      }

  private def dueReturnExists(obligation: Obligation): Return = {
    val dueReturnExists        = obligation.obligationDetails.exists(o =>
      o.status == Open && o.inboundCorrespondenceDueDate.isAfter(LocalDate.now())
    )
    val numberOfOverdueReturns = obligation.obligationDetails.count(o =>
      o.status == Open && o.inboundCorrespondenceDueDate.isBefore(LocalDate.now())
    )
    Return(dueReturnExists, numberOfOverdueReturns)
  }

  private def getError(message: String): Result = BadRequest(
    Json.obj(
      "error" -> message,
      "code"  -> "404"
    )
  )
}

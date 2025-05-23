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

import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.controllers.actions.{AuthorisedAction, CheckAppaIdAction}
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.service.AlcoholDutyService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton()
class AlcoholDutyController @Inject() (
  authorise: AuthorisedAction,
  checkAppaId: CheckAppaIdAction,
  alcoholDutyService: AlcoholDutyService,
  appConfig: AppConfig,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController {

  val returnPeriodPattern: Regex = """^(\d{2}A[A-L])$""".r

  def subscriptionSummary(alcoholDutyReference: String): Action[AnyContent] =
    (authorise andThen checkAppaId(alcoholDutyReference)).async { implicit request =>
      alcoholDutyService
        .getSubscriptionSummary(alcoholDutyReference)
        .fold(
          error,
          subscriptionSummary => Ok(Json.toJson(subscriptionSummary))
        )
    }

  def openObligationDetails(alcoholDutyReference: String, periodKey: String): Action[AnyContent] =
    (authorise andThen checkAppaId(alcoholDutyReference)).async { implicit request =>
      periodKey match {
        case returnPeriodPattern(_) =>
          alcoholDutyService
            .getOpenObligations(alcoholDutyReference, periodKey)
            .fold(
              err => error(err),
              obligationDetails => Ok(Json.toJson(obligationDetails))
            )
        case _                      => Future.successful(BadRequest(Json.toJson(ErrorResponse(BAD_REQUEST, "Invalid Period Key"))))
      }

    }

  def btaTileData(alcoholDutyReference: String): Action[AnyContent] =
    (authorise andThen checkAppaId(alcoholDutyReference)).async { implicit request =>
      if (appConfig.btaServiceAvailable) {
        alcoholDutyService
          .getAlcoholDutyCardData(alcoholDutyReference)
          .fold(
            error,
            alcoholDutyCardData => Ok(Json.toJson(alcoholDutyCardData))
          )
      } else {
        Future.successful(error(ErrorCodes.serviceUnavailable))
      }
    }

  def obligationDetails(alcoholDutyReference: String): Action[AnyContent] =
    (authorise andThen checkAppaId(alcoholDutyReference)).async { implicit request =>
      alcoholDutyService
        .getObligations(alcoholDutyReference, None)
        .fold(
          err => error(err),
          obligationDetails => Ok(Json.toJson(obligationDetails))
        )
    }
}

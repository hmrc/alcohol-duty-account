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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.alcoholdutyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.service.PaymentsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentsController @Inject() (
  authorise: AuthorisedAction,
  paymentsService: PaymentsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with Logging {

  def openPayments(appaId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      paymentsService
        .getOpenPayments(appaId)
        .fold(
          errorResponse => {
            logger.warn(s"Unable to get open payments for $appaId: $errorResponse")
            error(ErrorCodes.sanitiseError(errorResponse))
          },
          openPayments => Ok(Json.toJson(openPayments))
        )
    }

  def historicPayments(appaId: String, year: Int): Action[AnyContent] =
    authorise.async { implicit request =>
      paymentsService
        .getHistoricPayments(appaId, year)
        .fold(
          errorResponse => {
            logger.warn(s"Unable to get historic payments for $appaId: $errorResponse")
            error(ErrorCodes.sanitiseError(errorResponse))
          },
          historicPayments => Ok(Json.toJson(historicPayments))
        )
    }
}

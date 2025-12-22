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
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.controllers.actions.{AuthorisedAction, CheckAppaIdAction}
import uk.gov.hmrc.alcoholdutyaccount.service.{HistoricPaymentsRepositoryService, OpenPaymentsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, Year}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentsController @Inject() (
  authorise: AuthorisedAction,
  checkAppaId: CheckAppaIdAction,
  openPaymentsService: OpenPaymentsService,
  historicPaymentsRepositoryService: HistoricPaymentsRepositoryService,
  appConfig: AppConfig,
  cc: ControllerComponents,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with Logging {

  private lazy val minimumHistoricPaymentsYear: Int = appConfig.minimumHistoricPaymentsYear

  def openPayments(appaId: String): Action[AnyContent] =
    (authorise andThen checkAppaId(appaId)).async { implicit request =>
      openPaymentsService
        .getOpenPayments(appaId)
        .fold(
          errorResponse => {
            logger.warn(s"[PaymentsController] [openPayments] Unable to get open payments for $appaId: $errorResponse")
            error(errorResponse)
          },
          openPayments => Ok(Json.toJson(openPayments))
        )
    }

  def historicPayments(appaId: String): Action[AnyContent] =
    (authorise andThen checkAppaId(appaId)).async { implicit request =>
      val currentYear = Year.now(clock).getValue
      val minYear     = Math.max(currentYear - 6, minimumHistoricPaymentsYear)

      historicPaymentsRepositoryService.getAllYearsHistoricPayments(appaId, minYear, currentYear).map {
        case Left(errorResponse)           =>
          logger.warn(
            s"[PaymentsController] [historicPayments] Unable to get historic payments for $appaId: $errorResponse"
          )
          error(errorResponse)
        case Right(historicPaymentsByYear) => Ok(Json.toJson(historicPaymentsByYear))
      }
    }
}

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

import cats.data.EitherT
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.controllers.actions.{AuthorisedAction, CheckAppaIdAction}
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.service.{HistoricPaymentsService, OpenPaymentsService}
import uk.gov.hmrc.alcoholdutyaccount.utils.DateTimeHelper.instantToLocalDate
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsController @Inject() (
  authorise: AuthorisedAction,
  checkAppaId: CheckAppaIdAction,
  openPaymentsService: OpenPaymentsService,
  historicPaymentsService: HistoricPaymentsService,
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
            logger.warn(s"Unable to get open payments for $appaId: $errorResponse")
            error(errorResponse)
          },
          openPayments => Ok(Json.toJson(openPayments))
        )
    }

  private def validateYear(year: Int): EitherT[Future, ErrorResponse, Unit] =
    if (year < minimumHistoricPaymentsYear) {
      logger.info(s"Year requested is before $minimumHistoricPaymentsYear")
      EitherT.leftT(ErrorCodes.badRequest)
    } else if (year > instantToLocalDate(Instant.now(clock)).getYear) {
      logger.info(s"Year requested is after the current year")
      EitherT.leftT(ErrorCodes.badRequest)
    } else {
      EitherT.pure(())
    }

  def historicPayments(appaId: String, year: Int): Action[AnyContent] =
    (authorise andThen checkAppaId(appaId)).async { implicit request =>
      val historicPayments = for {
        _                <- validateYear(year)
        historicPayments <- historicPaymentsService.getHistoricPayments(appaId, year)
      } yield historicPayments

      historicPayments.fold(
        errorResponse => {
          logger.warn(s"Unable to get historic payments for $appaId: $errorResponse")
          error(errorResponse)
        },
        historicPayments => Ok(Json.toJson(historicPayments))
      )
    }
}

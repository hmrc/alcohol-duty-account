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
import uk.gov.hmrc.alcoholdutyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.alcoholdutyaccount.models.payments._
import uk.gov.hmrc.alcoholdutyaccount.utils.payments.{HistoricFinancialDataExtractor, OpenFinancialDataExtractor}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsService @Inject() (
  financialDataConnector: FinancialDataConnector,
  historicFinancialDataExtractor: HistoricFinancialDataExtractor,
  openFinancialDataExtractor: OpenFinancialDataExtractor
)(implicit ec: ExecutionContext)
    extends Logging {

  def getHistoricPayments(
    appaId: String,
    year: Int
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, HistoricPayments] =
    for {
      financialTransactionDocument <-
        financialDataConnector.getNotOnlyOpenFinancialData(appaId = appaId, year = year)
      historicPayments             <- historicFinancialDataExtractor.extractHistoricPayments(financialTransactionDocument)
    } yield HistoricPayments(year, historicPayments)

  def getOpenPayments(
    appaId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorResponse, OpenPayments] =
    for {
      financialTransactionDocument <- financialDataConnector.getOnlyOpenFinancialData(appaId)
      openPayments                 <- openFinancialDataExtractor.extractOpenPayments(financialTransactionDocument)
    } yield openFinancialDataExtractor.buildOpenPaymentsPayload(openPayments)

}

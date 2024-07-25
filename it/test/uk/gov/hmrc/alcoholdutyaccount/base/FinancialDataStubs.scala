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

package uk.gov.hmrc.alcoholdutyaccount.base

import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.common.{TestData, WireMockHelper}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument

trait FinancialDataStubs extends WireMockHelper with TestData { ISpecBase =>
  val config: AppConfig

  private val queryParams: Seq[(String, String)] = Seq(
    "onlyOpenItems"              -> "true",
    "includeLocks"               -> "false",
    "calculateAccruedInterest"   -> "false",
    "customerPaymentInformation" -> "false"
  )

  private def url(alcoholDutyReference: String): String =
    s"${config.financialDataHost}/enterprise/financial-data/${config.idType}/$alcoholDutyReference/${config.regime}"

  def stubGetFinancialData(
    alcoholDutyReference: String,
    financialTransactionDocument: FinancialTransactionDocument
  ): Unit =
    stubGetWithParameters(
      url(alcoholDutyReference),
      queryParams,
      OK,
      Json.toJson(financialTransactionDocument).toString()
    )

  def stubFinancialDataNotFound(alcoholDutyReference: String): Unit =
    stubGetWithParameters(url(alcoholDutyReference), queryParams, NOT_FOUND, "No financial data found")

  def stubFinancialDataWithFault(alcoholDutyReference: String): Unit =
    stubGetFaultWithParameters(url(alcoholDutyReference), queryParams)
}

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
import uk.gov.hmrc.alcoholdutyaccount.common.{AlcoholDutyTestData, WireMockHelper}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransactionDocument

trait FinancialDataStubs  extends WireMockHelper with AlcoholDutyTestData { ISpecBase =>
  val config: AppConfig

  private val url = s"${config.financialDataApiUrl}/enterprise/financial-data/${config.idType}/$alcoholDutyReference/${config.regimeType}"

  def stubGetFinancialData(financialTransactionDocument: FinancialTransactionDocument): Unit =
    stubGet(url, OK, Json.toJson(financialTransactionDocument).toString())

  def stubFinancialDataNotFound(): Unit =
    stubGet(url, NOT_FOUND, "No financial data found")
}
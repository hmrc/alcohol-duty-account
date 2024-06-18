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

import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.common.{AlcoholDutyTestData, WireMockHelper}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ObligationData, Open}

trait ObligationDataStubs extends WireMockHelper with AlcoholDutyTestData { ISpecBase =>
  val config: AppConfig

  private def url(alcoholDutyReference:String):String =
    s"${config.obligationDataApiUrl}/enterprise/obligation-data/${config.idType}/$alcoholDutyReference/${config.regimeType}"

  val queryParams = Seq("status" -> Open.value)

  val notFoundErrorMessage = """{
                               |    "code": "NOT_FOUND",
                               |    "reason": "The remote endpoint has indicated that no associated data found."
                               |}
                               |""".stripMargin

    val otherErrorMessage =
      """
        |{
        |    "code": "SERVICE_ERROR",
        |    "reason": "An error occurred"
        |}
        |""".stripMargin

  def stubGetObligations(alcoholDutyReference:String, obligationData: ObligationData): Unit =
    stubGetWithParameters(url(alcoholDutyReference), queryParams, OK, Json.toJson(obligationData).toString())

  def stubObligationsNotFound(alcoholDutyReference:String): Unit =
    stubGetWithParameters(url(alcoholDutyReference), queryParams, NOT_FOUND, notFoundErrorMessage)

  def stubObligationsError(alcoholDutyReference:String): Unit =
    stubGetWithParameters(url(alcoholDutyReference), queryParams, INTERNAL_SERVER_ERROR, "No obligation data found")
}
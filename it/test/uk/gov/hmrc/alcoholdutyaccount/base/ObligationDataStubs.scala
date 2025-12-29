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

import org.scalatest.Suite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.common.{TestData, WireMockHelper}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ObligationData, Open}

trait ObligationDataStubs extends WireMockHelper with TestData { 
  this: ISpecBase with Suite =>

  private def url(appaId: String): String = config.obligationDataUrl(appaId)

  private val queryParams = Seq("status" -> Open.value)

  private val notFoundErrorMessage: String = """{
                               |    "code": "NOT_FOUND",
                               |    "reason": "The remote endpoint has indicated that no associated data found."
                               |}
                               |""".stripMargin

  private val badRequestErrorMessage: String =
    """
      |{
      |     "code": "INVALID_DATE_RANGE",
      |     "reason": "The remote endpoint has indicated that an Invalid Date Range has been provided."
      |}
      |""".stripMargin

  private val multipleBadRequestErrorMessages: String =
    """
      |{
      |     "failures": [
      |       {
      |         "code": "INVALID_DATE_FROM",
      |         "reason": "Submission has not passed validation. Invalid parameter from."
      |       },
      |       {
      |         "code": "INVALID_DATE_TO",
      |         "reason": "Submission has not passed validation. Invalid parameter to."
      |       }
      |     ]
      |}
      |""".stripMargin

  def stubGetObligations(appaId: String, obligationData: ObligationData): Unit =
    stubGetWithParameters(url(appaId), queryParams, OK, Json.toJson(obligationData).toString())

  def stubObligationsNotFound(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, NOT_FOUND, notFoundErrorMessage)

  def stubObligationsBadRequest(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, BAD_REQUEST, badRequestErrorMessage)

  def stubObligationsMultipleErrorsBadRequest(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, BAD_REQUEST, multipleBadRequestErrorMessages)

  def stubObligationsWithFault(appaId: String): Unit =
    stubGetFaultWithParameters(url(appaId), queryParams)

  def stubObligationsInternalServerError(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, INTERNAL_SERVER_ERROR, "No obligation data found")
}

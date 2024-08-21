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

import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, UNPROCESSABLE_ENTITY}
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

  private val notFoundErrorMessage: String =
    """
      |{
      |     "code": "NOT_FOUND",
      |     "reason": "The remote endpoint has indicated that no data can be found."
      |}
      |""".stripMargin


  private val unprocessableEntityErrorMessage: String =
    """
      |{
      |     "code": "INVALID_DATA",
      |     "reason": "The remote endpoint has indicated that the request contains invalid data"
      |}
      |""".stripMargin

  private val badRequestErrorMessage: String =
    """
      |{
      |     "code": "INVALID_ONLYOPENITEMS",
      |     "reason": "Submission has not passed validation. Invalid parameter onlyOpenItems."
      |}
      |""".stripMargin

  private val multipleBadRequestErrorMessages: String =
    """
      |{
      |     "failures": [
      |       {
      |         "code": "INVALID_DATEFROM",
      |         "reason": "Submission has not passed validation. Invalid parameter dateFrom."
      |       },
      |       {
      |         "code": "INVALID_DATETO",
      |         "reason": "Submission has not passed validation. Invalid parameter dateTo."
      |       }
      |     ]
      |}
      |""".stripMargin


  private def url(appaId: String): String = config.financialDataUrl(appaId)

  def stubGetFinancialData(
                            appaId: String,
                            financialTransactionDocument: FinancialTransactionDocument
  ): Unit =
    stubGetWithParameters(
      url(appaId),
      queryParams,
      OK,
      Json.toJson(financialTransactionDocument).toString()
    )

  def stubFinancialDataNotFound(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, NOT_FOUND, notFoundErrorMessage)

  def stubFinancialDataUnprocessableEntity(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, UNPROCESSABLE_ENTITY, unprocessableEntityErrorMessage)

  def stubFinancialDataBadRequest(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, BAD_REQUEST, badRequestErrorMessage)

  def stubFinancialDataMultipleErrorsBadRequest(appaId: String): Unit =
    stubGetWithParameters(url(appaId), queryParams, BAD_REQUEST, multipleBadRequestErrorMessages)

  def stubFinancialDataWithFault(appaId: String): Unit =
    stubGetFaultWithParameters(url(appaId), queryParams)
}

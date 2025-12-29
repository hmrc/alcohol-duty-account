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

import play.api.http.Status.OK
import org.scalatest.Suite
import uk.gov.hmrc.alcoholdutyaccount.common.WireMockHelper

trait AuthStubs extends WireMockHelper {
  this: Suite =>
  
  val authUrl            = "/auth/authorise"
  val testAuthInternalId = "internalId"

  def authOKResponse(appaId: String): String =
    s"""|  {
        |    "authorisedEnrolments" : [ {
        |      "key" : "HMRC-AD-ORG",
        |      "identifiers" : [ {
        |        "key" : "APPAID",
        |        "value" : "$appaId"
        |      } ],
        |      "state" : "Activated",
        |      "confidenceLevel" : 50
        |    } ]
        |  }
         """.stripMargin

  def stubAuthorised(appaId: String): Unit =
    stubPost(authUrl, OK, authOKResponse(appaId))

  def verifyAuthorised(): Unit =
    verifyPost(authUrl)
}
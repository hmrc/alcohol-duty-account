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
import uk.gov.hmrc.alcoholdutyaccount.common.WireMockHelper

trait AuthStubs extends WireMockHelper {
  val authUrl            = "/auth/authorise"
  val testAuthInternalId = "internalId"

  val authOKResponse =
    s"""|{
        |  "internalId": "$testAuthInternalId",
        |  "loginTimes": {
        |     "currentLogin": "2016-11-27T09:00:00.000Z",
        |     "previousLogin": "2016-11-01T12:00:00.000Z"
        |  },
        |  "agentInformation": {},
        |  "confidenceLevel": 50
        |}
         """.stripMargin

  def stubAuthorised(): Unit =
    stubPost(authUrl, OK, authOKResponse)

  def verifyAuthorised(): Unit =
    verifyPost(authUrl)
}
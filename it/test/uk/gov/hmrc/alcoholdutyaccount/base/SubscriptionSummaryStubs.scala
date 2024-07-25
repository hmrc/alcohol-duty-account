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
import uk.gov.hmrc.alcoholdutyaccount.common.{TestData, WireMockHelper}
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{SubscriptionSummary, SubscriptionSummarySuccess}

trait SubscriptionSummaryStubs extends WireMockHelper with TestData { ISpecBase =>
  val config: AppConfig
  def url(appaId: String) = config.getSubscriptionUrl(appaId)

  def stubGetSubscriptionSummary(appaId: String, subscriptionSummary: SubscriptionSummary): Unit =
    stubGet(url(appaId), OK, Json.toJson(SubscriptionSummarySuccess(subscriptionSummary)).toString)

  def stubSubscriptionSummaryNotFound(appaId: String): Unit =
    stubGet(url(appaId), NOT_FOUND, "No subscription summary data found")

  def stubSubscriptionSummaryError(appaId: String): Unit =
    stubGet(url(appaId), INTERNAL_SERVER_ERROR, "An error occurred")

  def stubSubscriptionSummaryWithFault(appaId: String): Unit =
    stubGetFault(url(appaId))
}

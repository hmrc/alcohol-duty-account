/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.alcoholdutyaccount.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  val appName: String = config.get[String]("appName")

  private val subscriptionSummaryApiIdType       =
    config.get[String]("microservice.services.subscription-summary-api.id-type")
  private val subscriptionSummaryApiRegime       = config.get[String]("microservice.services.subscription-summary-api.regime")
  private val subscriptionSummaryApiHost: String =
    config.get[String]("microservice.services.subscription-summary-api.host")

  private val obligationDataApiIdType: String = config.get[String]("microservice.services.obligation-data-api.id-type")
  private val obligationDataApiRegime: String = config.get[String]("microservice.services.obligation-data-api.regime")
  private val obligationDataApiHost: String   = config.get[String]("microservice.services.obligation-data-api.host")

  def subscriptionSummaryApiUrl(alcoholReferenceId: String): String =
    s"$subscriptionSummaryApiHost/subscription/$subscriptionSummaryApiRegime/$subscriptionSummaryApiIdType/$alcoholReferenceId/summary"

  def obligationDataApiUrl(alcoholReferenceId: String): String =
    s"$obligationDataApiHost/enterprise/obligation-data/$obligationDataApiIdType/$alcoholReferenceId/$obligationDataApiRegime"
}

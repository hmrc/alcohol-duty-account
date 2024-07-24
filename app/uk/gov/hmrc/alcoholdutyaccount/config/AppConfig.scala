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
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.text.MessageFormat

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  val obligationDataHost: String            = servicesConfig.baseUrl("obligation")
  val obligationDataToken: String           = getConfStringAndThrowIfNotFound("obligation.token")
  val obligationDataEnv: String             = getConfStringAndThrowIfNotFound("obligation.env")
  val obligationDataFilterStartDate: String = getConfStringAndThrowIfNotFound("obligation.filterStartDate")

  val financialDataHost: String  = servicesConfig.baseUrl("financial")
  val financialDataToken: String = getConfStringAndThrowIfNotFound("obligation.token")
  val financialDataEnv: String   = getConfStringAndThrowIfNotFound("obligation.env")

  private val subscriptionHost: String                  = servicesConfig.baseUrl("subscription")
  lazy val subscriptionClientId: String                 = getConfStringAndThrowIfNotFound("subscription.clientId")
  lazy val subscriptionSecret: String                   = getConfStringAndThrowIfNotFound("subscription.secret")
  private lazy val subscriptionGetSubscriptionUrlFormat = new MessageFormat(
    getConfStringAndThrowIfNotFound("subscription.url.subscriptionSummary")
  )

  val idType: String = config.get[String]("downstream-apis.idType")
  val regime: String = config.get[String]("downstream-apis.regime")

  val enrolmentServiceName: String = config.get[String]("enrolment.serviceName")

  def getSubscriptionUrl(appaId: String): String = {
    val url = subscriptionGetSubscriptionUrlFormat.format(Array(regime, idType, appaId))
    s"$subscriptionHost$url"
  }

  private def getConfStringAndThrowIfNotFound(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"Could not find services config key '$key'"))
}

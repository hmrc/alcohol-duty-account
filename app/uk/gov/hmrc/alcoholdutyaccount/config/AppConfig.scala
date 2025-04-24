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

import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  private val obligationDataHost: String   = servicesConfig.baseUrl("obligation")
  val obligationDataToken: String          = getConfStringAndThrowIfNotFound("obligation.token")
  val obligationDataEnv: String            = getConfStringAndThrowIfNotFound("obligation.env")
  private lazy val obligationDataUrlPrefix = getConfStringAndThrowIfNotFound("obligation.url.obligationData")

  val obligationDataFilterStartDate: String = getConfStringAndThrowIfNotFound("obligation.filterStartDate")

  private val financialDataHost: String = servicesConfig.baseUrl("financial")
  val financialDataToken: String        = getConfStringAndThrowIfNotFound("financial.token")
  val financialDataEnv: String          = getConfStringAndThrowIfNotFound("financial.env")

  private lazy val financialDataUrlPrefix = getConfStringAndThrowIfNotFound("financial.url.financialData")

  val historicDataStartDate: String = getConfStringAndThrowIfNotFound("financial.historicData.dateFrom")
  val historicDataEndDate: String   = getConfStringAndThrowIfNotFound("financial.historicData.dateTo")

  private val subscriptionHost: String                  = servicesConfig.baseUrl("subscription")
  lazy val subscriptionClientId: String                 = getConfStringAndThrowIfNotFound("subscription.clientId")
  lazy val subscriptionSecret: String                   = getConfStringAndThrowIfNotFound("subscription.secret")
  private lazy val subscriptionGetSubscriptionUrlPrefix = getConfStringAndThrowIfNotFound(
    "subscription.url.subscriptionSummary"
  )

  val idType: String = config.get[String]("downstream-apis.idType")
  val regime: String = config.get[String]("downstream-apis.regime")

  val enrolmentServiceName: String = config.get[String]("enrolment.serviceName")
  val enrolmentIdentifierKey       = config.get[String]("enrolment.identifierKey")

  def getSubscriptionUrl(appaId: String): String =
    s"$subscriptionHost$subscriptionGetSubscriptionUrlPrefix/$regime/$idType/$appaId"

  def financialDataUrl(appaId: String): String =
    s"$financialDataHost$financialDataUrlPrefix/$idType/$appaId/$regime"

  def obligationDataUrl(appaId: String): String =
    s"$obligationDataHost$obligationDataUrlPrefix/$idType/$appaId/$regime"

  def minimumHistoricPaymentsYear: Int =
    config.get[Int]("payments.minimumHistoricPaymentsYear")

  def btaServiceAvailable: Boolean = config.get[Boolean]("features.bta-service-available")

  private[config] def getConfStringAndThrowIfNotFound(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"Could not find services config key '$key'"))

  // API retry attempts
  lazy val retryAttempts: Int                 = config.get[Int]("microservice.services.subscription.retry.retry-attempts")
  lazy val retryAttemptsDelay: FiniteDuration =
    config.get[FiniteDuration]("microservice.services.subscription.retry.retry-attempts-delay")
}

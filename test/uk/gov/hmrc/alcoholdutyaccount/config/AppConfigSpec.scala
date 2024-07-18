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

package uk.gov.hmrc.alcoholdutyaccount.config

import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase

class SpecBaseWithConfigOverrides extends SpecBase {
  override def configOverrides: Map[String, Any] = Map(
    "microservice.services.subscription.port"                    -> "http",
    "microservice.services.subscription.host"                    -> "host",
    "microservice.services.subscription.port"                    -> 12345,
    "microservice.services.subscription.url.subscriptionSummary" -> "/etmp/RESTAdapter/excise/subscriptionsummary/{0}/{1}/{2}",
    "downstream-apis.idType"                                     -> "ZAD",
    "downstream-apis.regime"                                     -> "AD"
  )
}

class AppConfigSpec extends SpecBaseWithConfigOverrides {
  "AppConfig" - {
    "should return the getSubscription url" in {
      appConfig.getSubscriptionUrl(
        appaId
      ) mustBe s"http://host:12345/etmp/RESTAdapter/excise/subscriptionsummary/AD/ZAD/$appaId"
    }
  }
}

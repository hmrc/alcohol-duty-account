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
    "appName"                                                    -> "appName",
    "microservice.services.subscription.protocol"                -> "http",
    "microservice.services.subscription.host"                    -> "subscriptionhost",
    "microservice.services.subscription.port"                    -> 12345,
    "microservice.services.subscription.clientId"                -> "subscription clientId",
    "microservice.services.subscription.secret"                  -> "subscription secret",
    "microservice.services.subscription.url.subscriptionSummary" -> "/etmp/RESTAdapter/excise/subscriptionsummary/{0}/{1}/{2}",
    "microservice.services.obligation.protocol"                  -> "http",
    "microservice.services.obligation.host"                      -> "obligationhost",
    "microservice.services.obligation.port"                      -> 54321,
    "microservice.services.obligation.token"                     -> "obligation token",
    "microservice.services.obligation.env"                       -> "obligation env",
    "microservice.services.obligation.url.obligationData"        -> "/enterprise/obligation-data/{0}/{1}/{2}",
    "microservice.services.obligation.filterStartDate"           -> "2023-09-01",
    "microservice.services.financial.protocol"                   -> "http",
    "microservice.services.financial.host"                       -> "financialhost",
    "microservice.services.financial.port"                       -> 2468,
    "microservice.services.financial.token"                      -> "financial token",
    "microservice.services.financial.env"                        -> "financial env",
    "microservice.services.financial.url.financialData"          -> "/enterprise/financial-data/{0}/{1}/{2}",
    "downstream-apis.idType"                                     -> "ZAD",
    "downstream-apis.regime"                                     -> "AD",
    "enrolment.serviceName"                                      -> "HMRC-AD-ORG"
  )
}

class AppConfigSpec extends SpecBaseWithConfigOverrides {
  "AppConfig" - {
    "should return the appName" in {
      appConfig.appName mustBe "appName"
    }

    "for subscriptions" - {
      "should return the getSubscription url" in {
        appConfig.getSubscriptionUrl(
          appaId
        ) mustBe s"http://subscriptionhost:12345/etmp/RESTAdapter/excise/subscriptionsummary/AD/ZAD/$appaId"
      }

      "should return the client id" in {
        appConfig.subscriptionClientId mustBe "subscription clientId"
      }

      "should return the secret" in {
        appConfig.subscriptionSecret mustBe "subscription secret"
      }
    }
  }

  "for obligations" - {
    "should return the obligation data url" in {
      appConfig.obligationDataUrl(
        appaId
      ) mustBe s"http://obligationhost:54321/enterprise/obligation-data/ZAD/$appaId/AD"
    }

    "should return the token" in {
      appConfig.obligationDataToken mustBe "obligation token"
    }

    "should return the env" in {
      appConfig.obligationDataEnv mustBe "obligation env"
    }

    "should return the filter start date" in {
      appConfig.obligationDataFilterStartDate mustBe "2023-09-01"
    }
  }

  "for financial data" - {
    "should return the financial data url" in {
      appConfig.financialDataUrl(
        appaId
      ) mustBe s"http://financialhost:2468/enterprise/financial-data/ZAD/$appaId/AD"
    }

    "should return the token" in {
      appConfig.financialDataToken mustBe "financial token"
    }

    "should return the env" in {
      appConfig.financialDataEnv mustBe "financial env"
    }
  }

  "should return the enrolment service name" in {
    appConfig.enrolmentServiceName mustBe "HMRC-AD-ORG"
  }

  "getConfStringAndThrowIfNotFound should" - {
    "return a key if found" in {
      appConfig.getConfStringAndThrowIfNotFound("subscription.secret") mustBe "subscription secret"
    }

    "throw an exception if not found" in {
      a[RuntimeException] shouldBe thrownBy(appConfig.getConfStringAndThrowIfNotFound("blah"))
    }
  }
}

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
    "microservice.services.subscription.url.subscriptionSummary" -> "/etmp/RESTAdapter/excise/subscriptionsummary",
    "microservice.services.circuit-breaker.max-failures"         -> 5,
    "microservice.services.circuit-breaker.call-timeout"         -> "11 seconds",
    "microservice.services.circuit-breaker.reset-timeout"        -> "1 minute",
    "microservice.services.retry.retry-attempts"                 -> 2,
    "microservice.services.retry.retry-attempts-delay"           -> "500 milliseconds",
    "microservice.services.obligation.protocol"                  -> "http",
    "microservice.services.obligation.host"                      -> "obligationhost",
    "microservice.services.obligation.port"                      -> 54321,
    "microservice.services.obligation.token"                     -> "obligation token",
    "microservice.services.obligation.env"                       -> "obligation env",
    "microservice.services.obligation.url.obligationData"        -> "/enterprise/obligation-data",
    "microservice.services.obligation.filterStartDate"           -> "2023-09-01",
    "microservice.services.financial.protocol"                   -> "http",
    "microservice.services.financial.host"                       -> "financialhost",
    "microservice.services.financial.port"                       -> 2468,
    "microservice.services.financial.token"                      -> "financial token",
    "microservice.services.financial.env"                        -> "financial env",
    "microservice.services.financial.url.financialData"          -> "/enterprise/financial-data",
    "downstream-apis.idType"                                     -> "ZAD",
    "downstream-apis.regime"                                     -> "AD",
    "downstream-apis.contractObjectType"                         -> "ZADP",
    "enrolment.serviceName"                                      -> "HMRC-AD-ORG",
    "payments.minimumHistoricPaymentsYear"                       -> 2024,
    "features.bta-service-available"                             -> true
  )
}

class AppConfigSpec extends SpecBaseWithConfigOverrides {
  "AppConfig" - {
    "must return the appName" in {
      appConfig.appName mustBe "appName"
    }

    "for subscriptions" - {
      "must return the getSubscription url" in {
        appConfig.getSubscriptionUrl(
          appaId
        ) mustBe s"http://subscriptionhost:12345/etmp/RESTAdapter/excise/subscriptionsummary/AD/ZAD/$appaId"
      }

      "must return the client id" in {
        appConfig.subscriptionClientId mustBe "subscription clientId"
      }

      "must return the secret" in {
        appConfig.subscriptionSecret mustBe "subscription secret"
      }
    }
  }

  "for obligations" - {
    "must return the obligation data url" in {
      appConfig.obligationDataUrl(
        appaId
      ) mustBe s"http://obligationhost:54321/enterprise/obligation-data/ZAD/$appaId/AD"
    }

    "must return the token" in {
      appConfig.obligationDataToken mustBe "obligation token"
    }

    "must return the env" in {
      appConfig.obligationDataEnv mustBe "obligation env"
    }

    "must return the filter start date" in {
      appConfig.obligationDataFilterStartDate mustBe "2023-09-01"
    }
  }

  "for financial data" - {
    "must return the financial data url" in {
      appConfig.financialDataUrl(
        appaId
      ) mustBe s"http://financialhost:2468/enterprise/financial-data/ZAD/$appaId/AD"
    }

    "must return the token" in {
      appConfig.financialDataToken mustBe "financial token"
    }

    "must return the env" in {
      appConfig.financialDataEnv mustBe "financial env"
    }
  }

  "must return the config relating to downstream APIs" - {
    "for idType" in {
      appConfig.idType mustBe "ZAD"
    }

    "for regime" in {
      appConfig.regime mustBe "AD"
    }
  }

  "must return the enrolment service name" in {
    appConfig.enrolmentServiceName mustBe "HMRC-AD-ORG"
  }

  "getConfStringAndThrowIfNotFound must" - {
    "return a key if found" in {
      appConfig.getConfStringAndThrowIfNotFound("subscription.secret") mustBe "subscription secret"
    }

    "throw an exception if not found" in {
      a[RuntimeException] mustBe thrownBy(appConfig.getConfStringAndThrowIfNotFound("blah"))
    }
  }

  "for payments" - {
    "minimumHistoricPaymentsYear must return a year" in {
      appConfig.minimumHistoricPaymentsYear mustBe 2024
    }
  }

  "for features" - {
    "btaServiceAvailable must return whether the service is available" in {
      appConfig.btaServiceAvailable mustBe true
    }
  }

  "for circuit-breaker" - {
    "max-failures must return the correct value" in {
      appConfig.maxFailures mustBe 5
    }

    "call-timeout must return the correct value" in {
      appConfig.callTimeout.toString() mustBe "11 seconds"
    }

    "reset-timeout must return the correct value" in {
      appConfig.resetTimeout.toString() mustBe "1 minute"
    }
  }

  "for retry" - {
    "retry-attempts must return the correct value" in {
      appConfig.retryAttempts mustBe 2
    }

    "retry-attempts-delay must return the correct value" in {
      appConfig.retryAttemptsDelay.toString() mustBe "500 milliseconds"
    }
  }

}

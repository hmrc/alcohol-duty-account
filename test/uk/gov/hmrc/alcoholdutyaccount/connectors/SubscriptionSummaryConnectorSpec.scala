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

package uk.gov.hmrc.alcoholdutyaccount.connectors

import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.alcoholdutyaccount.common.AlcoholDutyTestData
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionSummaryConnectorSpec extends SpecBase with ScalaFutures with ConnectorTestHelpers {
  protected val endpointName = "subscription"

  "SubscriptionSummaryConnector" - {
    "successfully get subscription summary data" in new SetUp {
      stubGet(url, OK, Json.toJson(approvedSubscriptionSummary).toString)
      whenReady(connector.getSubscriptionSummary(alcoholDutyReference).value) { result =>
        result mustBe Right(approvedSubscriptionSummary)
        verifyGet(url)
      }
    }

    "return an INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubGet(url, OK, "blah")
      whenReady(connector.getSubscriptionSummary(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary"))
        verifyGet(url)
      }
    }

    "return not found if subscription summary data cannot be found" in new SetUp {
      stubGet(url, NOT_FOUND, "")
      whenReady(connector.getSubscriptionSummary(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(NOT_FOUND, "Subscription summary not found"))
        verifyGet(url)
      }
    }

    "return an INTERNAL_SERVER_ERROR error if an error other than NOT_FOUND when fetching obligation data" in new SetUp {
      stubGet(url, BAD_REQUEST, "")
      whenReady(connector.getSubscriptionSummary(alcoholDutyReference).value) { result =>
        result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
        verifyGet(url)
      }
    }
  }

  class SetUp extends ConnectorFixture with AlcoholDutyTestData {
    val alcoholDutyReference: String = generateAlcoholDutyReference().sample.get
    val connector                    = new SubscriptionSummaryConnector(config = config, httpClient = httpClient)
    val url                          =
      s"${config.subscriptionApiUrl}/subscription/${config.regimeType}/${config.idType}/$alcoholDutyReference/summary"
  }
}

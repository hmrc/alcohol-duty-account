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

import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.alcoholdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.alcoholdutyaccount.models.hods.SubscriptionSummarySuccess
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class SubscriptionSummaryConnectorSpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "subscription"

  "SubscriptionSummaryConnector" - {
    "successfully get subscription summary data" in new SetUp {
      stubGet(url, OK, Json.toJson(SubscriptionSummarySuccess(approvedSubscriptionSummary)).toString)
      whenReady(connector.getSubscriptionSummary(appaId)) { result =>
        result mustBe Right(approvedSubscriptionSummary)
        verifyGet(url)
      }
    }

    "return BAD_REQUEST if a bad request received with no retry" in new SetUp {
      stubGet(url, BAD_REQUEST, Json.toJson(badRequest).toString)
      whenReady(connector.getSubscriptionSummary(appaId)) { result =>
        result mustBe Left(ErrorResponse(BAD_REQUEST, "Bad request"))
        verifyGetWithoutRetry(url)
      }
    }

    "return UNPROCESSABLE_ENTITY if a 422 is received with no retry" in new SetUp {
      stubGet(url, UNPROCESSABLE_ENTITY, Json.toJson(unprocessable).toString)
      whenReady(connector.getSubscriptionSummary(appaId)) { result =>
        result mustBe Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity"))
        verifyGetWithoutRetry(url)
      }
    }

    "return NOT_FOUND if subscription summary data cannot be found" in new SetUp {
      stubGet(url, NOT_FOUND, "")
      whenReady(connector.getSubscriptionSummary(appaId)) { result =>
        result mustBe Left(ErrorResponse(NOT_FOUND, "Subscription summary not found"))
        verifyGet(url)
      }
    }

    "return INTERNAL_SERVER_ERROR error" - {
      "if the data retrieved cannot be parsed" in new SetUp {
        stubGet(url, OK, "blah")
        whenReady(connector.getSubscriptionSummary(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary success"))
          verifyGet(url)
        }
      }

      "if an error other than BAD_REQUEST or NOT_FOUND or UNPROCESSABLE_ENTITY is returned" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
        whenReady(connector.getSubscriptionSummary(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGet(url)
        }
      }

      "if an error other than BAD_REQUEST or NOT_FOUND is returned" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
        whenReady(connector.getSubscriptionSummary(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGet(url)
        }
      }

      "if an error other than BAD_REQUEST or NOT_FOUND is returned the connector will invoke a retry" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
        whenReady(connectorWithRetry.getSubscriptionSummary(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGetRetry(url)
        }
      }

      "if an exception thrown when fetching subscription summary" in new SetUp {
        stubGetFault(url)
        whenReady(connector.getSubscriptionSummary(appaId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "An error occurred"))
          verifyGet(url)
        }
      }
    }
  }

  abstract class SetUp extends ConnectorFixture {
    val headers                                          = new HIPHeaders(fakeUUIDGenerator, appConfig, clock)
    val connector: SubscriptionSummaryConnector          = appWithHttpClientV2.injector.instanceOf[SubscriptionSummaryConnector]
    val connectorWithRetry: SubscriptionSummaryConnector =
      appWithHttpClientV2WithRetry.injector.instanceOf[SubscriptionSummaryConnector]
    lazy val url: String                                 = appConfig.getSubscriptionUrl(appaId)
  }
}

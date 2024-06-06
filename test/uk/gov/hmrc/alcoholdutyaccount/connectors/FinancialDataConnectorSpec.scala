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
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.alcoholdutyaccount.common.AlcoholDutyTestData

class FinancialDataConnectorSpec extends SpecBase with ScalaFutures with ConnectorTestHelpers {
  protected val endpointName = "financial"

  "FinancialDataConnector" - {
    "successfully get the financial transaction document" in new SetUp {
      stubGet(url, OK, Json.toJson(financialDocument).toString)
      whenReady(connector.getFinancialData(alcoholDutyReference).value) { result =>
        result mustBe Some(financialDocument)
        verifyGet(url)
      }
    }

    "return None if the data retrieved cannot be parsed" in new SetUp {
      stubGet(url, OK, "blah")
      whenReady(connector.getFinancialData(alcoholDutyReference).value) { result =>
        result mustBe None
        verifyGet(url)
      }
    }

    "return None if the financial transaction document cannot be found" in new SetUp {
      stubGet(url, NOT_FOUND, "")
      whenReady(connector.getFinancialData(alcoholDutyReference).value) { result =>
        result mustBe None
        verifyGet(url)
      }
    }

    "return None if an error other than NOT_FOUND when fetching the financial transaction document" in new SetUp {
      stubGet(url, BAD_REQUEST, "")
      whenReady(connector.getFinancialData(alcoholDutyReference).value) { result =>
        result mustBe None
        verifyGet(url)
      }
    }
  }

  class SetUp extends ConnectorFixture with AlcoholDutyTestData {
    val alcoholDutyReference: String = generateAlcoholDutyReference().sample.get
    val connector                    = new FinancialDataConnector(config = config, httpClient = httpClient)
    val url                          =
      s"${config.financialDataApiUrl}/enterprise/financial-data/${config.idType}/$alcoholDutyReference/${config.regimeType}"
  }
}

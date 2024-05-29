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

package uk.gov.hmrc.alcoholdutyaccount.models

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus._

class AlcoholDutyCardDataSpec extends SpecBase {

  "AlcoholDutyCardData" - {
    "should be able to be written as Json" - {
      "when all the fields are available " in {
        val alcoholDutyCardData = AlcoholDutyCardData(
          alcoholDutyReference = "REF01",
          approvalStatus = Some(Approved),
          hasSubscriptionSummaryError = false,
          hasReturnsError = false,
          hasPaymentError = false,
          returns = Returns(
            dueReturnExists = Some(true),
            numberOfOverdueReturns = Some(1)
          ),
          payments = Payments(
            balance = Some(
              Balance(
                totalPaymentAmount = 1001.99,
                isMultiplePaymentDue = false,
                chargeReference = Some("CHARGE-REF")
              )
            )
          )
        )

        val result = Json.toJson(alcoholDutyCardData)

        val expectedJson =
          """
            |{
            |  "alcoholDutyReference":"REF01",
            |  "approvalStatus":"Approved",
            |  "hasSubscriptionSummaryError":false,
            |  "hasReturnsError":false,
            |  "hasPaymentError":false,
            |  "returns": {
            |    "dueReturnExists":true,
            |    "numberOfOverdueReturns":1
            |  },
            |  "payments": {
            |     "balance": {
            |        "totalPaymentAmount":1001.99,
            |        "isMultiplePaymentDue":false,
            |        "chargeReference":"CHARGE-REF"
            |     }
            |  }
            |}""".stripMargin

        result shouldBe Json.parse(expectedJson)
      }

      Seq(Revoked, DeRegistered, SmallCiderProducer).foreach { approvalStatus =>
        s"when the Returns and Payments are empty and approval type is $approvalStatus" in {
          val alcoholDutyCardData = AlcoholDutyCardData(
            alcoholDutyReference = "REF01",
            approvalStatus = Some(approvalStatus),
            hasSubscriptionSummaryError = false,
            hasReturnsError = false,
            hasPaymentError = false,
            returns = Returns(),
            payments = Payments()
          )

          val result = Json.toJson(alcoholDutyCardData)

          val expectedJson =
            s"""
               |{
               |  "alcoholDutyReference":"REF01",
               |  "approvalStatus":"$approvalStatus",
               |  "hasSubscriptionSummaryError":false,
               |  "hasReturnsError":false,
               |  "hasPaymentError":false,
               |  "returns": {},
               |  "payments": {}
               |}""".stripMargin

          result shouldBe Json.parse(expectedJson)
        }
      }
      "when only mandatory available due to subscription summary error" in {
        val alcoholDutyCardData = AlcoholDutyCardData(
          alcoholDutyReference = "REF01",
          approvalStatus = None,
          hasSubscriptionSummaryError = true,
          hasReturnsError = false,
          hasPaymentError = false,
          returns = Returns(),
          payments = Payments()
        )

        val result = Json.toJson(alcoholDutyCardData)

        val expectedJson =
          """
            |{
            |  "alcoholDutyReference":"REF01",
            |  "hasSubscriptionSummaryError":true,
            |  "hasReturnsError":false,
            |  "hasPaymentError":false,
            |  "returns": {},
            |  "payments": {}
            |}""".stripMargin

        result shouldBe Json.parse(expectedJson)
      }

    }
  }
}

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

package uk.gov.hmrc.alcoholdutyaccount.models.hods

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase

class SubscriptionSummarySpec extends SpecBase {

  "SubscriptionSummary" - {
    "should be able to to be serialised from the full json" in {

      val expectedSubscriptionSummarySuccess = SubscriptionSummarySuccess(
        SubscriptionSummary(
          typeOfAlcoholApprovedFor = allApprovals,
          smallciderFlag = false,
          approvalStatus = Approved,
          insolvencyFlag = true
        )
      )

      val businessName = businessGen.sample.get

      val json =
        s"""
          |{   "success": {
          |        "processingDate":"2024-06-11T15:07:47.838Z",
          |        "organizationName":"$businessName",
          |        "typeOfAlcoholApprovedFor": [
          |            "01",
          |            "02",
          |            "03",
          |            "04",
          |            "05"
          |        ],
          |        "smallciderFlag": "0",
          |        "paperlessReference":"1",
          |        "emailAddress":"john.doe@example.com",
          |        "approvalStatus": "01",
          |        "insolvencyFlag": "1"
          |    }
          |}
          |""".stripMargin

      val result = Json.parse(json).asOpt[SubscriptionSummarySuccess]
      result shouldBe Some(expectedSubscriptionSummarySuccess)
    }

    Seq(
      (Approved, "01"),
      (DeRegistered, "02"),
      (Revoked, "03")
    ).foreach { case (approvalStatus, approvalCode) =>
      s"should be able to to be deserialise the approval status $approvalStatus from json" in {

        val expectedSubscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = false,
          approvalStatus = approvalStatus,
          insolvencyFlag = false
        )

        val json =
          s"""
            |{
            |    "typeOfAlcoholApprovedFor": ["01"],
            |    "smallciderFlag": "0",
            |    "approvalStatus": "$approvalCode",
            |    "insolvencyFlag": "0"
            |}
            |""".stripMargin

        val result = Json.parse(json).asOpt[SubscriptionSummary]
        result shouldBe Some(expectedSubscriptionSummary)
      }
    }

    "should throw an Exception if one of the the alcohol approved types cannot be deserialised" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedFor": ["06"],
          |    "smallciderFlag": "0",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if one of the the alcohol approved types is not a string" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedFor": [2],
          |    "smallciderFlag": "0",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if the Approval Status is not valid" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedFor": ["01"],
          |    "smallciderFlag": "0",
          |    "approvalStatus": "4",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if the Approval Status is not a string" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedFor": ["01"],
          |    "smallciderFlag": "0",
          |    "approvalStatus": 1,
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if a boolean is not valid" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedFor": ["01"],
          |    "smallciderFlag": "2",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }
  }
}

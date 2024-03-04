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
    "should be able to to be read from a Json" in {

      val expectedSubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
        smallCiderFlag = false,
        approvalStatus = Approved,
        insolvencyFlag = true
      )

      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": [
          |        "01",
          |        "02",
          |        "03",
          |        "04"
          |    ],
          |    "smallCiderFlag": "0",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "1"
          |}
          |""".stripMargin

      val result = Json.parse(json).asOpt[SubscriptionSummary]
      result shouldBe Some(expectedSubscriptionSummary)
    }

    Seq(
      (Approved, "01"),
      (DeRegistered, "02"),
      (Revoked, "03")
    ).foreach { case (approvalStatus, approvalCode) =>
      s"should be able to to be read from a Json if the approval status is: $approvalStatus" in {

        val expectedSubscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = false,
          approvalStatus = approvalStatus,
          insolvencyFlag = false
        )

        val json =
          s"""
            |{
            |    "typeOfAlcoholApprovedForList": ["01"],
            |    "smallCiderFlag": "0",
            |    "approvalStatus": "$approvalCode",
            |    "insolvencyFlag": "0"
            |}
            |""".stripMargin

        val result = Json.parse(json).asOpt[SubscriptionSummary]
        result shouldBe Some(expectedSubscriptionSummary)
      }
    }

    "should throw an Exception if one of the Type of Alcohol Approved is not valid" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": ["99"],
          |    "smallCiderFlag": "0",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if one of the Type of Alcohol Approved is not a string" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": [123],
          |    "smallCiderFlag": "0",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if the Approval Status is not a String" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": ["01"],
          |    "smallCiderFlag": "0",
          |    "approvalStatus": 123,
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if the Approval Status is not valid" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": ["01"],
          |    "smallCiderFlag": "0",
          |    "approvalStatus": "99",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if a boolean is not valid" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": ["01"],
          |    "smallCiderFlag": "2",
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }

    "should throw an Exception if a boolean is not a passed as string" in {
      val json =
        """
          |{
          |    "typeOfAlcoholApprovedForList": ["01"],
          |    "smallCiderFlag": 123,
          |    "approvalStatus": "01",
          |    "insolvencyFlag": "0"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[SubscriptionSummary]
    }
  }
}

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

class AlcoholDutyCardDataSpec extends SpecBase {

  "AlcoholDutyCardData" - {
    "should be able to be written as Json" in {
      val alcoholDutyCardData = AlcoholDutyCardData(
        alcoholDutyReference = "REF01",
        approvalStatus = Approved,
        hasReturnsError = false,
        returns = Return(
          dueReturnExists = Some(true),
          numberOfOverdueReturns = Some(1)
        )
      )

      val result = Json.toJson(alcoholDutyCardData)

      val expectedJson =
        """
          |{
          |  "alcoholDutyReference":"REF01",
          |  "approvalStatus":"Approved",
          |  "hasReturnsError":false,
          |  "returns": {
          |    "dueReturnExists":true,
          |    "numberOfOverdueReturns":1
          |  }
          |}""".stripMargin

      result shouldBe Json.parse(expectedJson)
    }

    Seq(Revoked, DeRegistered, Insolvent, SmallCiderProducer).foreach { approvalStatus =>
      s"should be able to be written as Json when the Return is empty and approval type is $approvalStatus" in {
        val alcoholDutyCardData = AlcoholDutyCardData(
          alcoholDutyReference = "REF01",
          approvalStatus = approvalStatus,
          hasReturnsError = false,
          returns = Return()
        )

        val result = Json.toJson(alcoholDutyCardData)

        val expectedJson =
          s"""
            |{
            |  "alcoholDutyReference":"REF01",
            |  "approvalStatus":"$approvalStatus",
            |  "hasReturnsError":false,
            |  "returns": {}
            |}""".stripMargin

        result shouldBe Json.parse(expectedJson)
      }
    }
  }
}

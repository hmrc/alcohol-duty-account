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

import java.time.LocalDate

class ObligationDataSpec extends SpecBase {

  "ObligationData" - {
    "should be able to to be read from a Json" in {
      val expectedObligationData = ObligationData(
        obligations = Seq(
          Obligation(
            obligationDetails = Seq(
              ObligationDetails(
                status = Open,
                inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
                inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
                inboundCorrespondenceDateReceived = None,
                inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
                periodKey = "24XY"
              ),
              ObligationDetails(
                status = Fulfilled,
                inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
                inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
                inboundCorrespondenceDateReceived = Some(LocalDate.of(2024, 1, 1)),
                inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
                periodKey = "24XY"
              )
            )
          )
        )
      )

      val json =
        """
          | {
          |    "obligations": [
          |        {
          |            "obligationDetails": [
          |                {
          |                    "status": "O",
          |                    "inboundCorrespondenceFromDate": "2024-01-01",
          |                    "inboundCorrespondenceToDate": "2024-01-01",
          |                    "inboundCorrespondenceDueDate": "2024-01-01",
          |                    "periodKey": "24XY"
          |                },
          |                {
          |                    "status": "F",
          |                    "inboundCorrespondenceFromDate": "2024-01-01",
          |                    "inboundCorrespondenceToDate": "2024-01-01",
          |                    "inboundCorrespondenceDateReceived": "2024-01-01",
          |                    "inboundCorrespondenceDueDate": "2024-01-01",
          |                    "periodKey": "24XY"
          |                }
          |            ]
          |        }
          |    ]
          |}
          |""".stripMargin

      val result = Json.parse(json).asOpt[ObligationData]
      result shouldBe Some(expectedObligationData)
    }

    "should throw an Exception if the Obligation Status is not Open or Fulfilled" in {
      val json =
        """
          |{
          |    "status": "N",
          |    "inboundCorrespondenceFromDate": "2024-01-01",
          |    "inboundCorrespondenceToDate": "2024-01-01",
          |    "inboundCorrespondenceDueDate": "2024-01-01",
          |    "periodKey": "24XY"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[ObligationDetails]
    }

    "should throw an Exception if the Obligation Status is not a string" in {
      val json =
        """
          |{
          |    "status": 123,
          |    "inboundCorrespondenceFromDate": "2024-01-01",
          |    "inboundCorrespondenceToDate": "2024-01-01",
          |    "inboundCorrespondenceDueDate": "2024-01-01",
          |    "periodKey": "24XY"
          |}
          |""".stripMargin

      an[JsResultException] should be thrownBy Json.parse(json).as[ObligationDetails]
    }
  }
}

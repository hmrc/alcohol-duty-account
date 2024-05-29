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

import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Beer, SubscriptionSummary}

class ApprovalStatusSpec extends SpecBase {

  "ApprovalStatus apply" - {
    " should return Deregistered" - {
      "when hods status is Deregistered" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = false,
          approvalStatus = hods.DeRegistered,
          insolvencyFlag = false
        )
        ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.DeRegistered
      }
      "when hods status is Deregistered regardless of the state of smallCiderFlag and insolvencyFlag" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = true,
          approvalStatus = hods.DeRegistered,
          insolvencyFlag = true
        )
        ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.DeRegistered
      }
    }
    "should return Revoked" - {
      "when hods status is Revoked" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = false,
          approvalStatus = hods.Revoked,
          insolvencyFlag = false
        )
        ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.Revoked
      }
      "when hods status is Revoked regardless of the state of smallCiderFlag and insolvencyFlag" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = true,
          approvalStatus = hods.Revoked,
          insolvencyFlag = true
        )
        ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.Revoked
      }
    }
    "should return SmallCiderProducer" - {
      "when hods status is Approved and smallCiderFlag is set" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = true,
          approvalStatus = hods.Approved,
          insolvencyFlag = false
        )
        ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.SmallCiderProducer
      }
      "when hods status is Approved and smallCiderFlag is set, regardless of the state of insolvencyFlag" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(Beer),
          smallCiderFlag = true,
          approvalStatus = hods.Approved,
          insolvencyFlag = true
        )
        ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.SmallCiderProducer
      }
    }
    "should return Insolvent when hods status is Approved and insolvencyFlag is set" in {
      val subscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = Set(Beer),
        smallCiderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = true
      )
      ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.Insolvent
    }
    "should return Approved when hods status is Approved and both smallCiderFlag and insolvencyFlag are false" in {
      val subscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = Set(Beer),
        smallCiderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = false
      )
      ApprovalStatus(subscriptionSummary) mustBe ApprovalStatus.Approved
    }
  }
}

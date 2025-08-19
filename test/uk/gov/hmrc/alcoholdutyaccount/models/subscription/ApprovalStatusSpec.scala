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

package uk.gov.hmrc.alcoholdutyaccount.models.subscription

import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.hods
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{Beer, SubscriptionSummary}

class ApprovalStatusSpec extends SpecBase {

  "ApprovalStatus apply" - {
    "must return DeRegistered" - {
      "when hods status is DeRegistered" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = false,
          approvalStatus = hods.DeRegistered,
          insolvencyFlag = false,
          paperlessReference = Some(true),
          bouncedEmailFlag = Some(false)
        )
        ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.DeRegistered
      }

      "when hods status is DeRegistered regardless of the state of smallciderFlag and insolvencyFlag" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = true,
          approvalStatus = hods.DeRegistered,
          insolvencyFlag = true,
          paperlessReference = Some(true),
          bouncedEmailFlag = Some(false)
        )
        ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.DeRegistered
      }
    }

    "must return Revoked" - {
      "when hods status is Revoked" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = false,
          approvalStatus = hods.Revoked,
          insolvencyFlag = false,
          paperlessReference = Some(true),
          bouncedEmailFlag = Some(false)
        )
        ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.Revoked
      }

      "when hods status is Revoked regardless of the state of smallciderFlag and insolvencyFlag" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = true,
          approvalStatus = hods.Revoked,
          insolvencyFlag = true,
          paperlessReference = Some(true),
          bouncedEmailFlag = Some(false)
        )
        ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.Revoked
      }
    }

    "must return SmallCiderProducer" - {
      "when hods status is Approved and smallciderFlag is set" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = true,
          approvalStatus = hods.Approved,
          insolvencyFlag = false,
          paperlessReference = Some(true),
          bouncedEmailFlag = Some(false)
        )
        ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.SmallCiderProducer
      }

      "when hods status is Approved and smallciderFlag is set, regardless of the state of insolvencyFlag" in {
        val subscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedFor = Set(Beer),
          smallciderFlag = true,
          approvalStatus = hods.Approved,
          insolvencyFlag = true,
          paperlessReference = Some(true),
          bouncedEmailFlag = Some(false)
        )
        ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.SmallCiderProducer
      }
    }

    "must return Insolvent when hods status is Approved and insolvencyFlag is set" in {
      val subscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedFor = Set(Beer),
        smallciderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = true,
        paperlessReference = Some(true),
        bouncedEmailFlag = Some(false)
      )
      ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.Insolvent
    }

    "must return Approved when hods status is Approved and both smallciderFlag and insolvencyFlag are false" in {
      val subscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedFor = Set(Beer),
        smallciderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = false,
        paperlessReference = Some(true),
        bouncedEmailFlag = Some(false)
      )
      ApprovalStatus.fromSubscriptionSummary(subscriptionSummary) mustBe ApprovalStatus.Approved
    }
  }
}

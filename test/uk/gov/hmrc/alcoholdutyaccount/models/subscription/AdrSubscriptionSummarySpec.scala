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

import cats.implicits._
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.hods
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.ApprovalStatus.{Approved, DeRegistered, Insolvent, Revoked, SmallCiderProducer}

class AdrSubscriptionSummarySpec extends SpecBase {
  "AdrSubscriptionSummary" - {
    "fromSubscriptionSummary" - {
      Seq(
        (hods.Beer, Set(AlcoholRegime.Beer)),
        (hods.CiderOrPerry, Set(AlcoholRegime.Cider)),
        (hods.Wine, Set(AlcoholRegime.Wine)),
        (hods.Spirits, Set(AlcoholRegime.Spirits)),
        (hods.OtherFermentedProduct, Set(AlcoholRegime.OtherFermentedProduct))
      ).foreach { case (approvedSubscription, expectedRegimes) =>
        s"must return alcohol regime ${expectedRegimes.map(_.entryName).mkString(",")} when the approved subscription is ${approvedSubscription.getClass.getTypeName}" in {
          val subscriptionSummary =
            approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = Set(approvedSubscription))

          val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary)

          adrSubscriptionSummary.map(_.regimes) mustBe Right(expectedRegimes)
        }
      }

      "must return all alcohol regimes when all subscriptions are approved" in {
        val subscriptionSummary    =
          approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = allApprovals)
        val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary)

        adrSubscriptionSummary.map(_.regimes) mustBe Right(allRegimes)
      }

      "must return a AdrSubscriptionSummary with Insolvent given a SubscriptionSummary with insolvency flag" in {
        val adrSubscriptionSummary =
          AdrSubscriptionSummary.fromSubscriptionSummary(approvedSubscriptionSummary.copy(insolvencyFlag = true))

        adrSubscriptionSummary.map(_.approvalStatus) mustBe Right(Insolvent)
      }

      "must return a AdrSubscriptionSummary with SmallCiderProducer status given a SubscriptionSummary with smallcider flag" in {
        val adrSubscriptionSummary =
          AdrSubscriptionSummary.fromSubscriptionSummary(approvedSubscriptionSummary.copy(smallciderFlag = true))

        adrSubscriptionSummary.map(_.approvalStatus) mustBe Right(SmallCiderProducer)
      }

      "must return a AdrSubscriptionSummary with Approved given a SubscriptionSummary with Approved status" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(approvedSubscriptionSummary)

        adrSubscriptionSummary.map(_.approvalStatus) mustBe Right(Approved)
      }

      "must return a AdrSubscriptionSummary with Revoked given a SubscriptionSummary with Revoked status" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(
          approvedSubscriptionSummary.copy(approvalStatus = hods.Revoked)
        )

        adrSubscriptionSummary.map(_.approvalStatus) mustBe Right(Revoked)
      }

      "must return a AdrSubscriptionSummary with DeRegistered given a SubscriptionSummary with DeRegistered status" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(
          approvedSubscriptionSummary.copy(approvalStatus = hods.DeRegistered)
        )

        adrSubscriptionSummary.map(_.approvalStatus) mustBe Right(DeRegistered)
      }

      "must return an INTERNAL_SERVER_ERROR if no alcohol types were approved" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(
          approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = Set.empty)
        )

        adrSubscriptionSummary.leftMap(_.statusCode) mustBe Left(INTERNAL_SERVER_ERROR)
      }
    }
  }
}

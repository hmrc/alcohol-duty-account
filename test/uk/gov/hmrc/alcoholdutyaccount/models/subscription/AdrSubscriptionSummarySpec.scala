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
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.hods
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.ApprovalStatus.{Approved, DeRegistered, Insolvent, Revoked, SmallCiderProducer}

class AdrSubscriptionSummarySpec extends SpecBase {
  "AdrSubscriptionSummary" - {
    "fromSubscriptionSummary" - {
      Seq(
        (hods.Beer, Set(AlcoholRegime.Beer)),
        (hods.CiderOrPerry, Set(AlcoholRegime.Cider, AlcoholRegime.OtherFermentedProduct)),
        (hods.Wine, Set(AlcoholRegime.Wine, AlcoholRegime.OtherFermentedProduct)),
        (hods.Spirits, Set(AlcoholRegime.Spirits)),
        (hods.OtherFermentedProduct, Set(AlcoholRegime.OtherFermentedProduct))
      ).foreach { case (approvedSubscription, expectedRegimes) =>
        s"should return an AdrSubscriptionSummary of ${expectedRegimes.map(_.entryName).mkString(",")} when ${approvedSubscription.getClass.getTypeName} is approved and OFP subscription is not available" in {
          val subscriptionSummary =
            approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = Set(approvedSubscription))

          val adrSubscriptionSummary = AdrSubscriptionSummary
            .fromSubscriptionSummary(subscriptionSummary, ofpAsSeparateRegimeEnabled = false)

          adrSubscriptionSummary.map(_.regimes) shouldBe Right(expectedRegimes)
        }
      }

      Seq(
        (hods.Beer, Set(AlcoholRegime.Beer)),
        (hods.CiderOrPerry, Set(AlcoholRegime.Cider)),
        (hods.Wine, Set(AlcoholRegime.Wine)),
        (hods.Spirits, Set(AlcoholRegime.Spirits)),
        (hods.OtherFermentedProduct, Set(AlcoholRegime.OtherFermentedProduct))
      ).foreach { case (approvedSubscription, expectedRegimes) =>
        s"should return an AdrSubscriptionSummary of ${expectedRegimes.map(_.entryName).mkString(",")} when ${approvedSubscription.getClass.getTypeName} is approved and OFP subscription is available" in {
          val subscriptionSummary =
            approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = Set(approvedSubscription))

          val adrSubscriptionSummary = AdrSubscriptionSummary
            .fromSubscriptionSummary(subscriptionSummary, ofpAsSeparateRegimeEnabled = true)

          adrSubscriptionSummary.map(_.regimes) shouldBe Right(expectedRegimes)
        }
      }

      "should return a AdrSubscriptionSummary with all regimes given a SubscriptionSummary with all alcohol other types approved when" - {
        "the feature toggle ofpAsSeparateRegimeEnabled is off" in {
          val subscriptionSummary =
            approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = allApprovals - hods.OtherFermentedProduct)
          val adrSubscriptionSummary = AdrSubscriptionSummary
            .fromSubscriptionSummary(subscriptionSummary, ofpAsSeparateRegimeEnabled = false)

          adrSubscriptionSummary.map(_.regimes) shouldBe Right(allRegimes)
        }

        "the feature toggle ofpAsSeparateRegimeEnabled is on" in {
          val adrSubscriptionSummary = AdrSubscriptionSummary
            .fromSubscriptionSummary(approvedSubscriptionSummary, ofpAsSeparateRegimeEnabled = true)

          adrSubscriptionSummary.map(_.regimes) shouldBe Right(allRegimes)
        }
      }

      "should return a AdrSubscriptionSummary with Insolvent given a SubscriptionSummary with insolvency flag" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary
          .fromSubscriptionSummary(
            approvedSubscriptionSummary.copy(insolvencyFlag = true),
            ofpAsSeparateRegimeEnabled = true
          )

        adrSubscriptionSummary.map(_.approvalStatus) shouldBe Right(Insolvent)
      }

      "should return a AdrSubscriptionSummary with SmallCiderProducer status given a SubscriptionSummary with smallcider flag" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary
          .fromSubscriptionSummary(
            approvedSubscriptionSummary.copy(smallciderFlag = true),
            ofpAsSeparateRegimeEnabled = true
          )

        adrSubscriptionSummary.map(_.approvalStatus) shouldBe Right(SmallCiderProducer)
      }

      "should return a AdrSubscriptionSummary with Approved given a SubscriptionSummary with Approved status" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary
          .fromSubscriptionSummary(approvedSubscriptionSummary, ofpAsSeparateRegimeEnabled = true)

        adrSubscriptionSummary.map(_.approvalStatus) shouldBe Right(Approved)
      }

      "should return a AdrSubscriptionSummary with Revoked given a SubscriptionSummary with Revoked status" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary
          .fromSubscriptionSummary(
            approvedSubscriptionSummary.copy(approvalStatus = hods.Revoked),
            ofpAsSeparateRegimeEnabled = true
          )

        adrSubscriptionSummary.map(_.approvalStatus) shouldBe Right(Revoked)
      }

      "should return a AdrSubscriptionSummary with DeRegistered given a SubscriptionSummary with DeRegistered status" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary
          .fromSubscriptionSummary(
            approvedSubscriptionSummary.copy(approvalStatus = hods.DeRegistered),
            ofpAsSeparateRegimeEnabled = true
          )

        adrSubscriptionSummary.map(_.approvalStatus) shouldBe Right(DeRegistered)
      }

      "should return an INTERNAL_SERVER_ERROR if no alcohol types were approved" in {
        val adrSubscriptionSummary = AdrSubscriptionSummary
          .fromSubscriptionSummary(
            approvedSubscriptionSummary.copy(typeOfAlcoholApprovedFor = Set.empty),
            ofpAsSeparateRegimeEnabled = true
          )

        adrSubscriptionSummary.leftMap(_.statusCode) shouldBe Left(INTERNAL_SERVER_ERROR)
      }
    }
  }
}

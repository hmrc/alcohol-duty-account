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
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus.{Approved, DeRegistered, Insolvent, Revoked, SmallCiderProducer}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.{ApprovalType, SubscriptionSummary}

class AdrSubscriptionSummarySpec extends SpecBase {

  "AdrSubscriptionSummary fromSubscriptionSummary" - {
    val alcoholRegimes: Set[ApprovalType] =
      Set(hods.Beer, hods.CiderOrPerry, hods.WineAndOtherFermentedProduct, hods.Spirits)
    val expectedRegimes                   = Set(
      AlcoholRegime.Beer,
      AlcoholRegime.Cider,
      AlcoholRegime.Spirits,
      AlcoholRegime.Wine,
      AlcoholRegime.OtherFermentedProduct
    )

    "should return a AdrSubscriptionSummary given a SubscriptionSummary with Approved status and all alcohol regimes" in {
      val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = alcoholRegimes,
        smallCiderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = false
      )
      val adrSubscriptionSummary                   = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).toOption.get

      adrSubscriptionSummary.approvalStatus shouldBe Approved
      adrSubscriptionSummary.regimes        shouldBe expectedRegimes
    }

    Seq(
      (hods.Beer, Set(AlcoholRegime.Beer)),
      (hods.CiderOrPerry, Set(AlcoholRegime.Cider, AlcoholRegime.OtherFermentedProduct)),
      (hods.WineAndOtherFermentedProduct, Set(AlcoholRegime.Wine, AlcoholRegime.OtherFermentedProduct)),
      (hods.Spirits, Set(AlcoholRegime.Spirits))
    ).foreach { case (approvedSubscription, expectedRegimes) =>
      s"should return an AdrSubscriptionSummary of ${expectedRegimes.map(_.entryName).mkString(",")} when ${approvedSubscription.getClass.getTypeName} is approved" in {
        val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
          typeOfAlcoholApprovedForList = Set(approvedSubscription),
          smallCiderFlag = false,
          approvalStatus = hods.Approved,
          insolvencyFlag = false
        )

        val adrSubscriptionSummary = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).toOption.get

        adrSubscriptionSummary.regimes shouldBe expectedRegimes
      }
    }

    "should return a AdrSubscriptionSummary given a SubscriptionSummary with Approved status with insolvent flag and all alcohol regimes" in {
      val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = alcoholRegimes,
        smallCiderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = true
      )
      val adrSubscriptionSummary                   = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).toOption.get

      adrSubscriptionSummary.approvalStatus shouldBe Insolvent
    }

    "should return a AdrSubscriptionSummary given a SubscriptionSummary with Approved status with small cider producer flag and all alcohol regimes" in {
      val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = alcoholRegimes,
        smallCiderFlag = true,
        approvalStatus = hods.Approved,
        insolvencyFlag = false
      )
      val adrSubscriptionSummary                   = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).toOption.get

      adrSubscriptionSummary.approvalStatus shouldBe SmallCiderProducer
    }

    "should return a AdrSubscriptionSummary given a SubscriptionSummary with Revoked status and all alcohol regimes" in {
      val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = alcoholRegimes,
        smallCiderFlag = false,
        approvalStatus = hods.Revoked,
        insolvencyFlag = false
      )
      val adrSubscriptionSummary                   = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).toOption.get

      adrSubscriptionSummary.approvalStatus shouldBe Revoked
    }

    "should return a AdrSubscriptionSummary given a SubscriptionSummary with DeRegistered status and all alcohol regimes" in {

      val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = alcoholRegimes,
        smallCiderFlag = false,
        approvalStatus = hods.DeRegistered,
        insolvencyFlag = false
      )
      val adrSubscriptionSummary                   = AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).toOption.get

      adrSubscriptionSummary.approvalStatus shouldBe DeRegistered
    }

    "should return an INTERNAL_SERVER_ERROR if no regimes were provided" in {
      val subscriptionSummary: SubscriptionSummary = SubscriptionSummary(
        typeOfAlcoholApprovedForList = Set.empty,
        smallCiderFlag = false,
        approvalStatus = hods.Approved,
        insolvencyFlag = false
      )
      val adrSubscriptionSummaryError              =
        AdrSubscriptionSummary.fromSubscriptionSummary(subscriptionSummary).swap.toOption.get

      adrSubscriptionSummaryError.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }
}

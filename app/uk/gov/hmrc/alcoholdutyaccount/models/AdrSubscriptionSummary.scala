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

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.alcoholdutyaccount.models.AlcoholRegime.{Beer, Cider, OtherFermentedProduct, Spirits, Wine}
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.SubscriptionSummary
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

case class AdrSubscriptionSummary(
  approvalStatus: ApprovalStatus,
  regimes: Set[AlcoholRegime]
)

object AdrSubscriptionSummary {

  def fromSubscriptionSummary(
    subscriptionSummary: SubscriptionSummary
  ): Either[ErrorResponse, AdrSubscriptionSummary] = {
    val regimes = mapRegimes(subscriptionSummary.typeOfAlcoholApprovedForList)

    if (regimes.isEmpty) {
      Left(ErrorResponse(500, "Expected at least one approved regime to be provided"))
    } else {
      Right(
        AdrSubscriptionSummary(
          mapStatus(subscriptionSummary),
          regimes
        )
      )
    }
  }

  private def mapRegimes(typeOfAlcohol: Set[hods.ApprovalType]): Set[AlcoholRegime] = typeOfAlcohol.flatMap {
    case hods.Beer                         => Seq(Beer)
    case hods.CiderOrPerry                 => Seq(Cider, OtherFermentedProduct)
    case hods.WineAndOtherFermentedProduct => Seq(Wine, OtherFermentedProduct)
    case hods.Spirits                      => Seq(Spirits)
  }

  private def mapStatus(subscriptionSummary: SubscriptionSummary): ApprovalStatus =
    subscriptionSummary.approvalStatus match {
      case hods.DeRegistered                       => DeRegistered
      case hods.Revoked                            => Revoked
      case _ if subscriptionSummary.smallCiderFlag => SmallCiderProducer
      case _ if subscriptionSummary.insolvencyFlag => Insolvent
      case hods.Approved                           => Approved
    }

  implicit val writes: OWrites[AdrSubscriptionSummary] = Json.writes[AdrSubscriptionSummary]
}

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

import play.api.libs.json.{Json, OWrites}
import AlcoholRegime.{Beer, Cider, OtherFermentedProduct, Spirits, Wine}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.alcoholdutyaccount.models.hods.SubscriptionSummary
import uk.gov.hmrc.alcoholdutyaccount.models.hods
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

case class AdrSubscriptionSummary(
  approvalStatus: ApprovalStatus,
  regimes: Set[AlcoholRegime]
)

object AdrSubscriptionSummary {

  def fromSubscriptionSummary(
    subscriptionSummary: SubscriptionSummary
  ): Either[ErrorResponse, AdrSubscriptionSummary] = {
    val regimes = mapRegimes(subscriptionSummary.typeOfAlcoholApprovedFor)

    if (regimes.isEmpty) {
      Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Expected at least one approved regime to be provided"))
    } else {
      Right(
        AdrSubscriptionSummary(
          ApprovalStatus.fromSubscriptionSummary(subscriptionSummary),
          regimes
        )
      )
    }
  }

  private def mapRegimes(typeOfAlcohol: Set[hods.ApprovalType]): Set[AlcoholRegime] = typeOfAlcohol.flatMap {
    case hods.Beer                  => Seq(Beer)
    case hods.CiderOrPerry          => Seq(Cider)
    case hods.Wine                  => Seq(Wine)
    case hods.Spirits               => Seq(Spirits)
    case hods.OtherFermentedProduct => Seq(OtherFermentedProduct)
  }

  implicit val writes: OWrites[AdrSubscriptionSummary] = Json.writes[AdrSubscriptionSummary]
}

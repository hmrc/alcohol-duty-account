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

import cats.data.NonEmptySet
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}
import uk.gov.hmrc.alcoholdutyaccount.models.AlcoholRegime.{Beer, Cider, OtherFermentedProduct, Spirits, Wine}
import uk.gov.hmrc.alcoholdutyaccount.models.ApprovalStatus._
import uk.gov.hmrc.alcoholdutyaccount.models.hods.SubscriptionSummary
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.collection.immutable.SortedSet

case class AdrSubscriptionSummary(
  approvalStatus: ApprovalStatus,
  regimes: NonEmptySet[AlcoholRegime]
)

object AdrSubscriptionSummary {

  def fromSubscriptionSummary(subscriptionSummary: SubscriptionSummary): Either[ErrorResponse, AdrSubscriptionSummary] =
    NonEmptySet
      .fromSet(SortedSet.from(mapRegimes(subscriptionSummary.typeOfAlcoholApprovedForList)))
      .fold[Either[ErrorResponse, AdrSubscriptionSummary]](
        Left(ErrorResponse(500, "Expected at least one approved regime to be provided"))
      )(regimes =>
        Right(
          AdrSubscriptionSummary(
            mapStatus(subscriptionSummary),
            regimes
          )
        )
      )

  private def mapRegimes(typeOfAlcohol: Set[hods.ApprovalType]): Set[AlcoholRegime] = typeOfAlcohol.flatMap {
    case hods.Beer                         => Seq(Beer)
    case hods.CiderOrPerry                 => Seq(Cider)
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

  implicit val writes: Writes[AdrSubscriptionSummary] =
    ((JsPath \ "approvalStatus").write[ApprovalStatus] and
      (JsPath \ "regimes").write[SortedSet[AlcoholRegime]])(a => (a.approvalStatus, a.regimes.toSortedSet))
}

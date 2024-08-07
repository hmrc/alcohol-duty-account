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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import uk.gov.hmrc.alcoholdutyaccount.models.hods
import uk.gov.hmrc.alcoholdutyaccount.models.hods.SubscriptionSummary

sealed trait ApprovalStatus extends EnumEntry
object ApprovalStatus extends Enum[ApprovalStatus] with PlayJsonEnum[ApprovalStatus] {
  val values = findValues

  case object Approved extends ApprovalStatus
  case object SmallCiderProducer extends ApprovalStatus
  case object Insolvent extends ApprovalStatus
  case object DeRegistered extends ApprovalStatus
  case object Revoked extends ApprovalStatus

  def fromSubscriptionSummary(subscriptionSummary: SubscriptionSummary): ApprovalStatus =
    subscriptionSummary.approvalStatus match {
      case hods.DeRegistered                                   => DeRegistered
      case hods.Revoked                                        => Revoked
      case hods.Approved if subscriptionSummary.smallciderFlag => SmallCiderProducer
      case hods.Approved if subscriptionSummary.insolvencyFlag => Insolvent
      case hods.Approved                                       => Approved
    }
}

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

import play.api.libs.json._
import uk.gov.hmrc.alcoholdutyaccount.models.JsonHelpers

sealed trait ApprovalType

case object Beer extends ApprovalType
case object CiderOrPerry extends ApprovalType
case object Wine extends ApprovalType
case object Spirits extends ApprovalType
case object OtherFermentedProduct extends ApprovalType

object ApprovalType {

  implicit val approvalTypeReads: Reads[ApprovalType] = {
    case JsString("01") => JsSuccess(Beer)
    case JsString("02") => JsSuccess(CiderOrPerry)
    case JsString("03") => JsSuccess(Wine)
    case JsString("04") => JsSuccess(Spirits)
    case JsString("05") => JsSuccess(OtherFermentedProduct)
    case s: JsString    => JsError(s"$s is not a valid ApprovalType")
    case v              => JsError(s"got $v was expecting a string representing a ApprovalType")
  }

  implicit val approvalTypeWrites: Writes[ApprovalType] = {
    case Beer                  => JsString("01")
    case CiderOrPerry          => JsString("02")
    case Wine                  => JsString("03")
    case Spirits               => JsString("04")
    case OtherFermentedProduct => JsString("05")
  }
}

sealed trait ApprovalStatus

case object Approved extends ApprovalStatus
case object DeRegistered extends ApprovalStatus
case object Revoked extends ApprovalStatus

object ApprovalStatus {
  implicit val approvalStatusReads: Reads[ApprovalStatus] = {
    case JsString("01") => JsSuccess(Approved)
    case JsString("02") => JsSuccess(DeRegistered)
    case JsString("03") => JsSuccess(Revoked)
    case s: JsString    => JsError(s"$s is not a valid ApprovalStatus")
    case v              => JsError(s"got $v was expecting a string representing a ApprovalStatus")
  }

  implicit val approvalStatusWrites: Writes[ApprovalStatus] = {
    case Approved     => JsString("01")
    case DeRegistered => JsString("02")
    case Revoked      => JsString("03")
  }
}

final case class SubscriptionSummarySuccess(success: SubscriptionSummary)

object SubscriptionSummarySuccess {
  implicit val subscriptionSummarySuccessFormat: OFormat[SubscriptionSummarySuccess] =
    Json.format[SubscriptionSummarySuccess]
}

final case class SubscriptionSummary(
  typeOfAlcoholApprovedFor: Set[ApprovalType],
  smallciderFlag: Boolean,
  approvalStatus: ApprovalStatus,
  insolvencyFlag: Boolean,
  paperlessReference: Option[Boolean],
  bouncedEmailFlag: Option[Boolean]
)

object SubscriptionSummary {
  import JsonHelpers.booleanReads
  import JsonHelpers.booleanWrites

  implicit val subscriptionSummaryFormat: OFormat[SubscriptionSummary] = Json.format[SubscriptionSummary]
}

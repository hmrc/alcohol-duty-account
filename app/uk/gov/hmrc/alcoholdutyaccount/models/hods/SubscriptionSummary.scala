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

sealed trait ApprovalType

case object Beer extends ApprovalType
case object CiderOrPerry extends ApprovalType
case object WineAndOtherFermentedProduct extends ApprovalType
case object Spirits extends ApprovalType

object ApprovalType {

  implicit val jsonReads: Reads[ApprovalType] = (json: JsValue) =>
    json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "01" => JsSuccess(Beer)
          case "02" => JsSuccess(CiderOrPerry)
          case "03" => JsSuccess(WineAndOtherFermentedProduct)
          case "04" => JsSuccess(Spirits)
          case s    => JsError(s"$s is not a valid ApprovalType")
        }
      case e: JsError          => e
    }
}

sealed trait ApprovalStatus

case object Approved extends ApprovalStatus
case object DeRegistered extends ApprovalStatus
case object Revoked extends ApprovalStatus

object ApprovalStatus {
  implicit val jsonReads: Reads[ApprovalStatus] = (json: JsValue) =>
    json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "01" => JsSuccess(Approved)
          case "02" => JsSuccess(DeRegistered)
          case "03" => JsSuccess(Revoked)
          case s    => JsError(s"$s is not a valid ApprovalStatus")
        }
      case e: JsError          => e
    }
}

final case class SubscriptionSummary(
  typeOfAlcoholApprovedForList: Set[ApprovalType],
  smallCiderFlag: Boolean,
  approvalStatus: ApprovalStatus,
  insolvencyFlag: Boolean
)

object SubscriptionSummary {

  implicit val booleanReads: Reads[Boolean] = {
    case JsString("0") => JsSuccess(false)
    case JsString("1") => JsSuccess(true)
    case s             => JsError(s"$s is not a valid Boolean")
  }

  implicit val reads: Reads[SubscriptionSummary] = Json.reads[SubscriptionSummary]
}

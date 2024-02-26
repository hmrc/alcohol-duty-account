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

import play.api.libs.json._

sealed trait ApprovalStatus

case object Approved extends ApprovalStatus
case object SmallCiderProducer extends ApprovalStatus
case object Insolvent extends ApprovalStatus
case object DeRegistered extends ApprovalStatus
case object Revoked extends ApprovalStatus

object ApprovalStatus {
  implicit val writes: Writes[ApprovalStatus] = {
    case Approved           => JsString("Approved")
    case SmallCiderProducer => JsString("SmallCiderProducer")
    case Insolvent          => JsString("Insolvent")
    case DeRegistered       => JsString("DeRegistered")
    case Revoked            => JsString("Revoked")
  }
}

final case class Return(dueReturnExists: Boolean, numberOfOverdueReturns: Int)
object Return {
  implicit val writes: Writes[Return] = Json.writes[Return]
}

final case class AlcoholDutyCardData(
  alcoholDutyReference: String,
  approvalStatus: ApprovalStatus,
  hasReturnsError: Boolean,
  returns: Seq[Return]
)

object AlcoholDutyCardData {
  implicit val writes: Writes[AlcoholDutyCardData] = Json.writes[AlcoholDutyCardData]
}

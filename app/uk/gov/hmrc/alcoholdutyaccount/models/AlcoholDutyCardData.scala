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

final case class Payments(
  totalPaymentAmount: Option[BigDecimal] = None,
  isMultiplePaymentDue: Option[Boolean] = None,
  chargeReference: Option[String] = None
)

object Payments {
  implicit val writes: Writes[Payments] = Json.writes[Payments]
}

final case class Returns(
  dueReturnExists: Option[Boolean] = None,
  numberOfOverdueReturns: Option[Int] = None
)
object Returns {
  implicit val writes: Writes[Returns] = Json.writes[Returns]
}

object InsolventCardData {
  def apply(alcoholDutyReference: String): AlcoholDutyCardData = AlcoholDutyCardData(
    alcoholDutyReference = alcoholDutyReference,
    approvalStatus = Insolvent,
    hasReturnsError = false,
    hasPaymentError = false,
    returns = Returns(),
    payments = Payments()
  )
}

case class AlcoholDutyCardData(
  alcoholDutyReference: String,
  approvalStatus: ApprovalStatus,
  hasReturnsError: Boolean,
  hasPaymentError: Boolean,
  returns: Returns,
  payments: Payments
)

object AlcoholDutyCardData {
  implicit val writes: Writes[AlcoholDutyCardData] = Json.writes[AlcoholDutyCardData]
}

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
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.{AdrSubscriptionSummary, ApprovalStatus}

final case class Balance(
  totalPaymentAmount: BigDecimal,
  isMultiplePaymentDue: Boolean,
  chargeReference: Option[String]
)

object Balance {
  implicit val writes: Writes[Balance] = Json.writes[Balance]
}

final case class Payments(
  balance: Option[Balance] = None
)

object Payments {
  implicit val writes: Writes[Payments] = Json.writes[Payments]
}

final case class Returns(
  dueReturnExists: Option[Boolean] = None,
  numberOfOverdueReturns: Option[Int] = None,
  periodKey: Option[String] = None
)
object Returns {
  implicit val writes: Writes[Returns] = Json.writes[Returns]
}

object RestrictedCardData {
  def apply(alcoholDutyReference: String, subscriptionSummary: AdrSubscriptionSummary): AlcoholDutyCardData =
    AlcoholDutyCardData(
      alcoholDutyReference = alcoholDutyReference,
      approvalStatus = Some(subscriptionSummary.approvalStatus),
      hasSubscriptionSummaryError = false,
      hasReturnsError = false,
      hasPaymentsError = false,
      returns = Returns(),
      payments = Payments(),
      contactPreference = subscriptionSummary.paperlessReference.map {
        case true  => "digital"
        case false => "paper"
      },
      emailBounced = subscriptionSummary.paperlessReference.map { _ =>
        subscriptionSummary.bouncedEmailFlag.contains(true)
      }
    )
}

case class AlcoholDutyCardData(
  alcoholDutyReference: String,
  approvalStatus: Option[ApprovalStatus],
  hasSubscriptionSummaryError: Boolean,
  hasReturnsError: Boolean,
  hasPaymentsError: Boolean,
  returns: Returns,
  payments: Payments,
  contactPreference: Option[String],
  emailBounced: Option[Boolean]
)

object AlcoholDutyCardData {
  implicit val writes: Writes[AlcoholDutyCardData] = Json.writes[AlcoholDutyCardData]
}

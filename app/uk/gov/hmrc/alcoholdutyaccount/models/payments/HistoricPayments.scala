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

package uk.gov.hmrc.alcoholdutyaccount.models.payments

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.alcoholdutyaccount.models.ReturnPeriod
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class HistoricPayment(
  period: ReturnPeriod,
  transactionType: TransactionType,
  chargeReference: Option[String],
  amountPaid: BigDecimal
)

object HistoricPayment {
  implicit val historicPaymentFormat: OFormat[HistoricPayment] = Json.format[HistoricPayment]
}

case class HistoricPayments(
  year: Int,
  payments: Seq[HistoricPayment]
)

object HistoricPayments {
  implicit val historicPaymentsFormat: OFormat[HistoricPayments] = Json.format[HistoricPayments]
}

case class UserHistoricPayments(
  appaId: String,
  historicPaymentsData: Seq[HistoricPayments],
  createdAt: Instant
)

object UserHistoricPayments {
  implicit val format: OFormat[UserHistoricPayments] =
    (
      (__ \ "_id").format[String] and
        (__ \ "historicPaymentsData").format[Seq[HistoricPayments]] and
        (__ \ "createdAt").format(MongoJavatimeFormats.instantFormat)
    )(UserHistoricPayments.apply, unlift(UserHistoricPayments.unapply))
}

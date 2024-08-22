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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

sealed trait OpenPayment

case class OutstandingPayment(
  transactionType: TransactionType,
  dueDate: LocalDate,
  chargeReference: Option[String],
  remainingAmount: BigDecimal
) extends OpenPayment

object OutstandingPayment {
  implicit val outstandingPaymentFormat: OFormat[OutstandingPayment] = Json.format[OutstandingPayment]
}

case class UnallocatedPayment(
  paymentDate: LocalDate,
  unallocatedAmount: BigDecimal
) extends OpenPayment

object UnallocatedPayment {
  implicit val unallocatedPaymentFormat: OFormat[UnallocatedPayment] = Json.format[UnallocatedPayment]
}

case class OpenPayments(
  outstandingPayments: Seq[OutstandingPayment],
  totalOutstandingPayments: BigDecimal,
  unallocatedPayments: Seq[UnallocatedPayment],
  totalUnallocatedPayments: BigDecimal,
  totalOpenPaymentsAmount: BigDecimal
)

object OpenPayments {
  implicit val openPaymentsFormat: OFormat[OpenPayments] = Json.format[OpenPayments]
}

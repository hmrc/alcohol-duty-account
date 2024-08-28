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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait TransactionType extends EnumEntry

object TransactionType extends Enum[TransactionType] with PlayJsonEnum[TransactionType] {
  val values = findValues

  case object Return extends TransactionType

  case object PaymentOnAccount extends TransactionType

  case object LPI extends TransactionType

  case object RPI extends TransactionType

  def fromMainTransactionType(mainTransactionType: String): Option[TransactionType] = mainTransactionType match {
    case "6074" => Some(Return)
    case "0060" => Some(PaymentOnAccount)
    case "6075" => Some(LPI)
    case "6076" => Some(RPI)
    case _      => None
  }

  def toMainTransactionType(transactionType: TransactionType): String = transactionType match {
    case Return           => "6074"
    case PaymentOnAccount => "0060"
    case LPI              => "6075"
    case RPI              => "6076"
  }

  def isRPI(mainTransactionType: String): Boolean = mainTransactionType == "6076"

  def isPaymentOnAccount(mainTransactionType: String): Boolean = mainTransactionType == "0060"
}

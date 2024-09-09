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

sealed trait TransactionType extends EnumEntry {
  val mainTransactionType: String
}

object TransactionType extends Enum[TransactionType] with PlayJsonEnum[TransactionType] {
  val values = findValues

  case object Return extends TransactionType {
    val mainTransactionType: String = "6074"
  }

  case object PaymentOnAccount extends TransactionType {
    val mainTransactionType: String = "0060"
  }

  case object LPI extends TransactionType {
    val mainTransactionType: String = "6075"
  }

  case object RPI extends TransactionType {
    val mainTransactionType: String = "6076"
  }

  def fromMainTransactionType(mainTransactionType: String): Option[TransactionType] = mainTransactionType match {
    case Return.mainTransactionType           => Some(Return)
    case PaymentOnAccount.mainTransactionType => Some(PaymentOnAccount)
    case LPI.mainTransactionType              => Some(LPI)
    case RPI.mainTransactionType              => Some(RPI)
    case _                                    => None
  }

  def toMainTransactionType(transactionType: TransactionType): String = transactionType.mainTransactionType

  def isRPI(mainTransactionType: String): Boolean = mainTransactionType == RPI.mainTransactionType

  def isPaymentOnAccount(mainTransactionType: String): Boolean =
    mainTransactionType == PaymentOnAccount.mainTransactionType
}

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

import play.api.libs.json.{Format, Json}

case class FinancialTransactionItem(
  subItem: String,
  amount: BigDecimal
)

object FinancialTransactionItem {
  implicit val format: Format[FinancialTransactionItem] = Json.format[FinancialTransactionItem]
}

case class FinancialTransaction(
  sapDocumentNumber: String,
  periodKey: Option[String],
  chargeReference: Option[String],
  originalAmount: BigDecimal,
  mainTransaction: String,
  subTransaction: String,
  outstandingAmount: Option[BigDecimal],
  items: Seq[FinancialTransactionItem]
)

object FinancialTransaction {
  implicit val format: Format[FinancialTransaction] = Json.format[FinancialTransaction]
}
case class FinancialTransactionDocument(financialTransactions: Seq[FinancialTransaction])

object FinancialTransactionDocument {
  implicit val format: Format[FinancialTransactionDocument] = Json.format[FinancialTransactionDocument]
}

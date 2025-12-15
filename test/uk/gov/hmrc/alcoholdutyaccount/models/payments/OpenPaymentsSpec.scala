/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ReturnPeriod

class OpenPaymentsSpec extends SpecBase {
  "OpenPayments" - {
    val openPayments = OpenPayments(
      outstandingPayments = Seq(
        OutstandingPayment(
          taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          transactionType = TransactionType.Return,
          dueDate = singleFullyOutstandingReturn.financialTransactions.head.items.head.dueDate.get,
          chargeReference = Some(chargeReference),
          remainingAmount = BigDecimal("9000")
        )
      ),
      totalOutstandingPayments = BigDecimal("9000"),
      unallocatedPayments = Seq(
        UnallocatedPayment(
          paymentDate = singleOverpayment.financialTransactions.head.items.head.dueDate.get,
          unallocatedAmount = BigDecimal("-1000")
        )
      ),
      totalUnallocatedPayments = BigDecimal("-1000"),
      totalOpenPaymentsAmount = BigDecimal("8000")
    )

    val json =
      s"""{"outstandingPayments":[{"taxPeriodFrom":"2024-05-01","taxPeriodTo":"2024-05-31","transactionType":"Return","dueDate":"2024-06-25","chargeReference":"$chargeReference","remainingAmount":9000}],"totalOutstandingPayments":9000,"unallocatedPayments":[{"paymentDate":"2024-06-25","unallocatedAmount":-1000}],"totalUnallocatedPayments":-1000,"totalOpenPaymentsAmount":8000}"""

    "must serialise to json" in {
      Json.toJson(openPayments).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[OpenPayments] mustBe openPayments
    }
  }
}

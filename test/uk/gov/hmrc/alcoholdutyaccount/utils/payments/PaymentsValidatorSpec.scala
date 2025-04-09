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

package uk.gov.hmrc.alcoholdutyaccount.utils.payments

import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes

class PaymentsValidatorSpec extends SpecBase {
  "PaymentsValidator" - {
    "when calling validateAndGetFinancialTransactionData" - {
      "and there is no financial transactions for a document which must not be possible" - {
        "it must return an error gracefully (coverage)" in new SetUp {
          val sapDocumentNumber: String = sapDocumentNumberGen.sample.get

          paymentsValidator.validateAndGetFinancialTransactionData(
            sapDocumentNumber,
            Seq.empty
          ) mustBe Left(
            ErrorCodes.unexpectedResponse
          )
        }
      }
    }
  }

  class SetUp {
    val paymentsValidator: PaymentsValidator = new PaymentsValidator()
  }
}

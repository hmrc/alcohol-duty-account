/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.{ErrorCodes, ReturnPeriod}
import uk.gov.hmrc.alcoholdutyaccount.models.hods.FinancialTransaction
import uk.gov.hmrc.alcoholdutyaccount.common.TestData
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{FinancialTransactionData, TransactionType}

import java.time.LocalDate

class PaymentsValidatorSpec extends SpecBase with TestData {
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

      if (appConfig.isOfficerAssessment) {
        "allows to have multiple tax periods for outstanding payments of officers assessment in diff tax periods" in new SetUp {

          val sapDocumentNumber: String = sapDocumentNumberGen.sample.get

          paymentsValidator.validateAndGetFinancialTransactionData(
            sapDocumentNumber,
            createOfficerAssessmentDoc(sapDocumentNumber, periodKey, periodKey2).financialTransactions
          ) mustBe Right(
            FinancialTransactionData(
              transactionType = TransactionType.OfficerAssessment,
              maybePeriodKey = Option(periodKey),
              taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
              taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
              dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
              maybeChargeReference = Option(chargeReference),
              maybeRunningLock = None
            )
          )
        }

        "allows to have multiple tax periods for outstanding payments of officers assessment LPI in diff tax peirods" in new SetUp {

          val sapDocumentNumber: String = sapDocumentNumberGen.sample.get

          paymentsValidator.validateAndGetFinancialTransactionData(
            sapDocumentNumber,
            createOfficerAssessmentDoc(
              sapDocumentNumber,
              periodKey,
              periodKey2,
              TransactionType.OfficerAssessmentLPI
            ).financialTransactions
          ) mustBe Right(
            FinancialTransactionData(
              transactionType = TransactionType.OfficerAssessmentLPI,
              maybePeriodKey = Option(periodKey),
              taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
              taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
              dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
              maybeChargeReference = Option(chargeReference),
              maybeRunningLock = None
            )
          )
        }

        "allows a running Lock to show if present" in new SetUp {
          val sapDocumentNumber: String = sapDocumentNumberGen.sample.get

          paymentsValidator.validateAndGetFinancialTransactionData(
            sapDocumentNumber,
            createOfficerAssessmentDoc(
              sapDocumentNumber,
              periodKey,
              periodKey2,
              TransactionType.OfficerAssessment
            ).financialTransactions
          ) mustBe Right(
            FinancialTransactionData(
              transactionType = TransactionType.OfficerAssessment,
              maybePeriodKey = Option(periodKey),
              taxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
              taxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
              dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
              maybeChargeReference = Option(chargeReference),
              maybeRunningLock = None
            )
          )
        }
      }
    }
  }

  class SetUp {
    val paymentsValidator: PaymentsValidator = new PaymentsValidator(appConfig)
  }
}

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

package uk.gov.hmrc.alcoholdutyaccount.common

import uk.gov.hmrc.alcoholdutyaccount.models.hods._

import java.time.LocalDate

trait AlcoholDutyTestData {

  val testInternalId: String = "internalId"

  val alcoholDutyReference: String = "XAD1234567890"

  val approvedSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
    smallCiderFlag = false,
    approvalStatus = Approved,
    insolvencyFlag = false
  )

  val insolventSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
    smallCiderFlag = false,
    approvalStatus = Approved,
    insolvencyFlag = true
  )

  val deregisteredSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
    smallCiderFlag = false,
    approvalStatus = DeRegistered,
    insolvencyFlag = false
  )

  val revokedSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
    smallCiderFlag = false,
    approvalStatus = Revoked,
    insolvencyFlag = false
  )

  val obligationData = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(
          ObligationDetails(
            status = Open,
            inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
            inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
            inboundCorrespondenceDateReceived = None,
            inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
            periodKey = "24XY"
          ),
          ObligationDetails(
            status = Fulfilled,
            inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
            inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(2024, 1, 1)),
            inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
            periodKey = "24XY"
          )
        )
      )
    )
  )

  val financialDocument = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        periodKey = "18AA",
        chargeReference = "X1234567890",
        originalAmount = 1000.00,
        outstandingAmount = 50.00,
        mainTransaction = "1001",
        subTransaction = "1111",
        items = Seq(
          FinancialTransactionItem(
            subItem = "001",
            paymentAmount = 50.00
          )
        )
      ),
      FinancialTransaction(
        periodKey = "18AA",
        chargeReference = "X1234567890",
        originalAmount = 1000.00,
        outstandingAmount = 50.00,
        mainTransaction = "1001",
        subTransaction = "2222",
        items = Seq(
          FinancialTransactionItem(
            subItem = "002",
            paymentAmount = 100.00
          )
        )
      )
    )
  )

}

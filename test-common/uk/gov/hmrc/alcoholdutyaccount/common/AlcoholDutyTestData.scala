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

import uk.gov.hmrc.alcoholdutyaccount.models.{AdrObligationData, AdrSubscriptionSummary, AlcoholRegime, ApprovalStatus, ObligationStatus, ReturnPeriod}
import uk.gov.hmrc.alcoholdutyaccount.models.hods._

import java.time.LocalDate

trait AlcoholDutyTestData {

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

  val alcoholDutyReference = "XMADP0000000200"

  val periodKey  = "24AE"
  val periodKey2 = "24AF"
  val periodKey3 = "24AG"
  val periodKey4 = "24AH"

  val returnPeriod  = ReturnPeriod(periodKey, 2024, 5)
  val returnPeriod2 = ReturnPeriod(periodKey2, 2024, 6)
  val returnPeriod3 = ReturnPeriod(periodKey3, 2024, 7)
  val returnPeriod4 = ReturnPeriod(periodKey4, 2024, 8)

  val obligationDetails = ObligationDetails(
    status = Open,
    inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
    inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
    inboundCorrespondenceDateReceived = None,
    inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
    periodKey = periodKey
  )

  val obligationDetails2 = obligationDetails.copy(periodKey = periodKey2)
  val obligationDetails3 = obligationDetails.copy(periodKey = periodKey3)

  val obligationDataSingleOpen = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(obligationDetails)
      )
    )
  )

  val obligationDataMultipleOpen = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(obligationDetails, obligationDetails2)
      ),
      Obligation(
        obligationDetails = Seq(obligationDetails3)
      )
    )
  )

  val fulfilledObligationDetails = ObligationDetails(
    status = Fulfilled,
    inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
    inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
    inboundCorrespondenceDateReceived = None,
    inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
    periodKey = periodKey
  )

  val obligationDataSingleFulfilled = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetails)
      )
    )
  )

  val emptyFinancialDocument = FinancialTransactionDocument(financialTransactions = Seq.empty)

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

  val approvedAdrSubscriptionSummary = new AdrSubscriptionSummary(
    approvalStatus = ApprovalStatus.Approved,
    regimes = Set(
      AlcoholRegime.Beer,
      AlcoholRegime.Wine,
      AlcoholRegime.Cider,
      AlcoholRegime.Spirits,
      AlcoholRegime.OtherFermentedProduct
    )
  )

  val adrObligationDetails          = new AdrObligationData(ObligationStatus.Open)
  val adrObligationDetailsFulfilled = new AdrObligationData(ObligationStatus.Fulfilled)
}

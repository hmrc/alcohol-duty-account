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

import org.scalacheck.Gen
import uk.gov.hmrc.alcoholdutyaccount.models.{AdrObligationData, AdrSubscriptionSummary, AlcoholRegime, ApprovalStatus, ObligationStatus}
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

  val smallCiderProducerSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedForList = Set(Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits),
    smallCiderFlag = true,
    approvalStatus = Approved,
    insolvencyFlag = false
  )

  def generateAlcoholDutyReference(): Gen[String] = for {
    idNumSection <- Gen.listOfN(10, Gen.numChar)
  } yield s"XMADP${idNumSection.mkString}"

  def generateProductKey(): Gen[String] = for {
    year  <- Gen.listOfN(2, Gen.numChar)
    month <- Gen.chooseNum(0, 11)
  } yield s"${year}A${(month + 'A').toChar}"

  val periodKey  = "24AE"
  val periodKey2 = "24AF"
  val periodKey3 = "24AG"
  val periodKey4 = "24AH"

  val obligationFilter = Open

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

  val obligationDataSingleFulfilled          = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetails)
      )
    )
  )
  val obligationDataMultipleOpenAndFulfilled = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(obligationDetails, obligationDetails2)
      ),
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

  val adrObligationDetails          = new AdrObligationData(
    status = ObligationStatus.Open,
    fromDate = LocalDate.of(2024, 1, 1),
    toDate = LocalDate.of(2024, 1, 1),
    dueDate = LocalDate.of(2024, 1, 1),
    periodKey
  )
  val adrObligationDetailsOpen2     = new AdrObligationData(
    status = ObligationStatus.Open,
    fromDate = LocalDate.of(2024, 1, 1),
    toDate = LocalDate.of(2024, 1, 1),
    dueDate = LocalDate.of(2024, 1, 1),
    periodKey2
  )
  val adrObligationDetailsFulfilled = new AdrObligationData(
    ObligationStatus.Fulfilled,
    fromDate = LocalDate.of(2024, 1, 1),
    toDate = LocalDate.of(2024, 1, 1),
    dueDate = LocalDate.of(2024, 1, 1),
    periodKey
  )

  val adrMultipleOpenAndFulfilledData =
    Seq(adrObligationDetails, adrObligationDetailsOpen2, adrObligationDetailsFulfilled)

  val adrMultipleOpenData =
    Seq(adrObligationDetails, adrObligationDetailsOpen2)
}

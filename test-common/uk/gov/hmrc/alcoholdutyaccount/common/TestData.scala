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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.alcoholdutyaccount.common.generators.ModelGenerators
import uk.gov.hmrc.alcoholdutyaccount.models.{AdrObligationData, ObligationStatus}
import uk.gov.hmrc.alcoholdutyaccount.models.hods._
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.{AdrSubscriptionSummary, AlcoholRegime, ApprovalStatus}

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait TestData extends ModelGenerators {
  val clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of("UTC"))

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val appaId: String = appaIdGen.sample.get

  val allApprovals = Set[ApprovalType](Beer, CiderOrPerry, WineAndOtherFermentedProduct, Spirits)

  val approvedSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedFor = allApprovals,
    smallciderFlag = false,
    approvalStatus = Approved,
    insolvencyFlag = false
  )

  val insolventSubscriptionSummary = approvedSubscriptionSummary.copy(insolvencyFlag = true)

  val deregisteredSubscriptionSummary = approvedSubscriptionSummary.copy(approvalStatus = DeRegistered)

  val revokedSubscriptionSummary = approvedSubscriptionSummary.copy(approvalStatus = Revoked)

  val smallCiderProducerSubscriptionSummary = approvedSubscriptionSummary.copy(smallciderFlag = true)

  def generateProductKey(): Gen[String] = for {
    year  <- Gen.listOfN(2, Gen.numChar)
    month <- Gen.chooseNum(0, 11)
  } yield s"${year}A${(month + 'A').toChar}"

  val periodKey  = "24AE"
  val periodKey2 = "24AF"
  val periodKey3 = "24AG"
  val periodKey4 = "24AH"

  val obligationFilterOpen      = Open
  val obligationFilterFulfilled = Fulfilled

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

  val obligationDataEmpty = ObligationData(
    obligations = Seq.empty
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
            amount = 50.00
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
            amount = 100.00
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

  case class DownstreamErrorDetails(code: String, message: String, logID: String)

  object DownstreamErrorDetails {
    implicit val downstreamErrorDetailsWrites: OFormat[DownstreamErrorDetails] = Json.format[DownstreamErrorDetails]
  }

  val badRequest          = DownstreamErrorDetails("400", "You messed up", "id")
  val internalServerError = DownstreamErrorDetails("500", "Computer says No!", "id")
}

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
import uk.gov.hmrc.alcoholdutyaccount.models.hods._
import uk.gov.hmrc.alcoholdutyaccount.models.payments.TransactionType._
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{HistoricPayment, HistoricPayments, OpenPayments, TransactionType, UserHistoricPayments}
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.ContactPreferenceForBTA.Digital
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.{AdrSubscriptionSummary, AlcoholRegime, ApprovalStatus}
import uk.gov.hmrc.alcoholdutyaccount.models.{AdrObligationData, FulfilledObligations, ObligationStatus, ReturnPeriod, UserFulfilledObligations}

import java.time.{Clock, Instant, LocalDate, Month, YearMonth, ZoneId}

trait TestData extends ModelGenerators {
  val clock     = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of("UTC"))
  val clock2025 = Clock.fixed(Instant.ofEpochMilli(1755864000000L), ZoneId.of("UTC")) // 22 August 2025

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val appaId: String          = appaIdGen.sample.get
  val businessPartner: String = businessPartnerGen.sample.get
  val contractAccount: String = contractAccountGen.sample.get
  val contractObject: String  = contractObjectGen.sample.get

  val allRegimes: Set[AlcoholRegime] = Set(
    AlcoholRegime.Beer,
    AlcoholRegime.Cider,
    AlcoholRegime.Spirits,
    AlcoholRegime.Wine,
    AlcoholRegime.OtherFermentedProduct
  )

  val allApprovals = Set[ApprovalType](Beer, CiderOrPerry, Wine, Spirits, OtherFermentedProduct)

  val approvedSubscriptionSummary = SubscriptionSummary(
    typeOfAlcoholApprovedFor = allApprovals,
    smallciderFlag = false,
    approvalStatus = Approved,
    insolvencyFlag = false,
    paperlessReference = true,
    bouncedEmailFlag = Some(false)
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

  val obligationDetails = ObligationDetails(
    status = Open,
    inboundCorrespondenceFromDate = LocalDate.of(2024, 1, 1),
    inboundCorrespondenceToDate = LocalDate.of(2024, 1, 1),
    inboundCorrespondenceDateReceived = None,
    inboundCorrespondenceDueDate = LocalDate.of(2024, 1, 1),
    periodKey = periodKey
  )

  val obligationDetails2            = obligationDetails.copy(periodKey = periodKey2)
  val obligationDetails3            = obligationDetails.copy(periodKey = periodKey3)
  val obligationDetailsFromFuture   =
    obligationDetails.copy(inboundCorrespondenceToDate = LocalDate.now(clock).plusDays(1000))
  val obligationDetailsFromToday    =
    obligationDetails.copy(inboundCorrespondenceToDate = LocalDate.now(clock).minusDays(1))
  val obligationDetailsFromTomorrow = obligationDetails.copy(inboundCorrespondenceToDate = LocalDate.now(clock))

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

  val openObligationDataFromToday = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(obligationDetailsFromToday)
      )
    )
  )

  val openObligationDataFromTomorrow = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(obligationDetailsFromTomorrow)
      )
    )
  )

  val openObligationDataFromFuture = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(obligationDetailsFromFuture)
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

  val fulfilledObligationDetailsFromFuture   =
    fulfilledObligationDetails.copy(inboundCorrespondenceToDate = LocalDate.now(clock).plusDays(1000))
  val fulfilledObligationDetailsFromToday    =
    fulfilledObligationDetails.copy(inboundCorrespondenceToDate = LocalDate.now(clock).minusDays(1))
  val fulfilledObligationDetailsFromTomorrow =
    fulfilledObligationDetails.copy(inboundCorrespondenceToDate = LocalDate.now(clock))

  val obligationDataSingleFulfilled   = ObligationData(
    obligations = Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetails)
      )
    )
  )
  val obligationDataMultipleFulfilled = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetails, fulfilledObligationDetails.copy(periodKey = periodKey2))
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

  val fulfilledObligationDataFromToday = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetailsFromToday)
      )
    )
  )

  val fulfilledObligationDataFromTomorrow = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetailsFromTomorrow)
      )
    )
  )

  val fulfilledObligationDataFromFuture = ObligationData(obligations =
    Seq(
      Obligation(
        obligationDetails = Seq(fulfilledObligationDetailsFromFuture)
      )
    )
  )

  val noObligations = ObligationData.noObligations

  val emptyFinancialDocument = FinancialTransactionDocument.emptyDocument

  val financialDocumentWithSingleSapDocumentNo = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        sapDocumentNumber = "123456",
        periodKey = Some("18AA"),
        taxPeriodFrom = Some(LocalDate.of(2018, 1, 1)),
        taxPeriodTo = Some(LocalDate.of(2018, 1, 31)),
        chargeReference = Some("X1234567890"),
        contractObjectType = Some("ZADP"),
        originalAmount = BigDecimal(1000),
        outstandingAmount = Some(BigDecimal("50")),
        clearedAmount = Some(BigDecimal("950")),
        mainTransaction = "1001",
        subTransaction = "1111",
        items = Seq(
          FinancialTransactionItem(
            subItem = "001",
            dueDate = None,
            amount = 50.00
          )
        )
      ),
      FinancialTransaction(
        sapDocumentNumber = "123456",
        periodKey = Some("18AA"),
        taxPeriodFrom = Some(LocalDate.of(2018, 1, 1)),
        taxPeriodTo = Some(LocalDate.of(2018, 1, 31)),
        chargeReference = Some("X1234567890"),
        contractObjectType = Some("ZADP"),
        originalAmount = BigDecimal(1000),
        outstandingAmount = Some(BigDecimal("50")),
        clearedAmount = Some(BigDecimal("950")),
        mainTransaction = "1001",
        subTransaction = "2222",
        items = Seq(
          FinancialTransactionItem(
            subItem = "002",
            dueDate = None,
            amount = 100.00
          )
        )
      )
    )
  )

  def createFinancialDocument(
    onlyOpenItems: Boolean,
    sapDocumentNumber: String,
    originalAmount: BigDecimal,
    maybeOutstandingAmount: Option[BigDecimal],
    dueDate: LocalDate,
    transactionType: TransactionType = Return,
    maybePeriodKey: Option[String] = None,
    maybeTaxPeriodFrom: Option[LocalDate] = None,
    maybeTaxPeriodTo: Option[LocalDate] = None,
    maybeChargeReference: Option[String] = None
  ) = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumber,
        periodKey = maybePeriodKey,
        taxPeriodFrom = maybeTaxPeriodFrom,
        taxPeriodTo = maybeTaxPeriodTo,
        chargeReference = maybeChargeReference,
        contractObjectType = Some("ZADP"),
        originalAmount = originalAmount,
        outstandingAmount = maybeOutstandingAmount,
        clearedAmount = maybeOutstandingAmount.fold[Option[BigDecimal]](Some(originalAmount))(outstandingAmount =>
          if (outstandingAmount == originalAmount) None else Some(originalAmount - outstandingAmount)
        ),
        mainTransaction = toMainTransactionType(transactionType),
        subTransaction = "6132",
        items = if (maybeOutstandingAmount.isEmpty || maybeOutstandingAmount.contains(originalAmount)) {
          Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(dueDate),
              amount = originalAmount
            )
          )
        } else if (onlyOpenItems) {
          Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(dueDate),
              amount = maybeOutstandingAmount.get
            )
          )
        } else {
          Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(dueDate),
              amount = maybeOutstandingAmount.get
            ),
            FinancialTransactionItem(
              subItem = "001",
              dueDate = Some(dueDate),
              amount = originalAmount - maybeOutstandingAmount.get
            )
          )
        }
      )
    )
  )

  def combineFinancialTransactionDocuments(docs: Seq[FinancialTransactionDocument]): FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = docs.map(_.financialTransactions).reduce(_ ++ _)
    )

  val singleFullyOutstandingReturn: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = true,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("9000"),
      maybeOutstandingAmount = Some(BigDecimal("9000")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.Return,
      maybePeriodKey = Some(periodKey),
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  def singlePartiallyOutstandingReturn(onlyOpenItems: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = onlyOpenItems,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("9000"),
      maybeOutstandingAmount = Some(BigDecimal("5000")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.Return,
      maybePeriodKey = Some(periodKey),
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  val singlePaidReturn: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("9000"),
      maybeOutstandingAmount = None,
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.Return,
      maybePeriodKey = Some(periodKey),
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  val singleRefundedReturn: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("-9000"),
      maybeOutstandingAmount = None,
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.Return,
      maybePeriodKey = Some(periodKey),
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  def twoLineItemPartiallyOutstandingReturn(onlyOpenItems: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("9000"),
          maybeOutstandingAmount = Some(BigDecimal("5000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = Some(chargeReference)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("2000"),
          maybeOutstandingAmount = Some(BigDecimal("2000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = Some(chargeReference)
        )
      )
    )
  }

  // No outstanding amounts as line items cancel
  val nilReturnLineItemsCancelling: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = false,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("9000"),
          maybeOutstandingAmount = Some(BigDecimal("9000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = Some(chargeReference)
        ),
        createFinancialDocument(
          onlyOpenItems = false,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("-9000"),
          maybeOutstandingAmount = Some(BigDecimal("-9000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = Some(chargeReference)
        )
      )
    )
  }

  def twoSeparateOutstandingReturnsOnePartiallyPaid(onlyOpenItems: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber  = sapDocumentNumberGen.sample.get
    val chargeReference    = chargeReferenceGen.sample.get
    val sapDocumentNumber2 = sapDocumentNumberGen.sample.get
    val chargeReference2   = chargeReferenceGen.sample.get

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("9000"),
          maybeOutstandingAmount = Some(BigDecimal("5000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = Some(chargeReference)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber2,
          originalAmount = BigDecimal("2000"),
          maybeOutstandingAmount = Some(BigDecimal("2000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey2),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodToDate()),
          maybeChargeReference = Some(chargeReference2)
        )
      )
    )
  }

  val twoSeparateReturnsOneFullyPaid: FinancialTransactionDocument = {
    val sapDocumentNumber  = sapDocumentNumberGen.sample.get
    val chargeReference    = chargeReferenceGen.sample.get
    val sapDocumentNumber2 = sapDocumentNumberGen.sample.get
    val chargeReference2   = chargeReferenceGen.sample.get

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = false,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("9000"),
          maybeOutstandingAmount = None,
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = Some(chargeReference)
        ),
        createFinancialDocument(
          onlyOpenItems = false,
          sapDocumentNumber = sapDocumentNumber2,
          originalAmount = BigDecimal("2000"),
          maybeOutstandingAmount = Some(BigDecimal("2000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some(periodKey2),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodToDate()),
          maybeChargeReference = Some(chargeReference2)
        )
      )
    )
  }

  val singleOverpayment: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("-9000"),
      maybeOutstandingAmount = Some(BigDecimal("-9000")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.Overpayment,
      maybeChargeReference = Some(chargeReference)
    )
  }

  def twoSeparateOverpayments(onlyOpenItems: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber  = sapDocumentNumberGen.sample.get
    val chargeReference    = chargeReferenceGen.sample.get
    val sapDocumentNumber2 = sapDocumentNumberGen.sample.get
    val chargeReference2   = chargeReferenceGen.sample.get

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("-5000"),
          maybeOutstandingAmount = Some(BigDecimal("-5000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
          transactionType = TransactionType.Overpayment,
          maybeChargeReference = Some(chargeReference)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber2,
          originalAmount = BigDecimal("-2000"),
          maybeOutstandingAmount = Some(BigDecimal("-2000")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate(),
          transactionType = TransactionType.Overpayment,
          maybeChargeReference = Some(chargeReference2)
        )
      )
    )
  }

  def twoUnallocatedSeparateOverpayments(onlyOpenItems: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val dueDate           = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("-5000"),
          maybeOutstandingAmount = Some(BigDecimal("-5000")),
          dueDate = dueDate,
          transactionType = TransactionType.Overpayment,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = None
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("-2000"),
          maybeOutstandingAmount = Some(BigDecimal("-2000")),
          dueDate = dueDate,
          transactionType = TransactionType.Overpayment,
          maybePeriodKey = Some(periodKey2),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodToDate()),
          maybeChargeReference = None
        )
      )
    )
  }

  def twoAllocatedSeparateOverpayments(onlyOpenItems: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val dueDate           = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()

    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("-5000"),
          maybeOutstandingAmount = Some(BigDecimal("-1000")),
          dueDate = dueDate,
          transactionType = TransactionType.Overpayment,
          maybePeriodKey = Some(periodKey),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
          maybeChargeReference = None
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumber,
          originalAmount = BigDecimal("-5000"),
          maybeOutstandingAmount = Some(BigDecimal("-1000")),
          dueDate = dueDate,
          transactionType = TransactionType.Overpayment,
          maybePeriodKey = Some(periodKey2),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).periodToDate()),
          maybeChargeReference = None
        )
      )
    )
  }
  val singleFullyOutstandingLPI: FinancialTransactionDocument                                = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("50"),
      maybeOutstandingAmount = Some(BigDecimal("50")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.LPI,
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  val singleRPI: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("-50"),
      maybeOutstandingAmount = Some(BigDecimal("-50")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.RPI,
      maybeChargeReference = Some(chargeReference)
    )
  }

  val singleCA: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("2000"),
      maybeOutstandingAmount = Some(BigDecimal("2000")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.CA,
      maybePeriodKey = Some(periodKey),
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  val singleCAI: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get

    createFinancialDocument(
      onlyOpenItems = false,
      sapDocumentNumber = sapDocumentNumber,
      originalAmount = BigDecimal("20"),
      maybeOutstandingAmount = Some(BigDecimal("20")),
      dueDate = ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate(),
      transactionType = TransactionType.CAI,
      maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
      maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodToDate()),
      maybeChargeReference = Some(chargeReference)
    )
  }

  def multipleStatuses(onlyOpenItems: Boolean): FinancialTransactionDocument =
    combineFinancialTransactionDocuments(
      Seq(
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("237.44"),
          maybeOutstandingAmount = Some(BigDecimal("237.44")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AH").dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some("24AH"),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AH").periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AH").periodToDate()),
          maybeChargeReference = Some(chargeReferenceGen.sample.get)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("4577.44"),
          maybeOutstandingAmount = Some(BigDecimal("4577.44")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AE").dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some("24AE"),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AE").periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AE").periodToDate()),
          maybeChargeReference = Some(chargeReferenceGen.sample.get)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("4577.44"),
          maybeOutstandingAmount = Some(BigDecimal("2577.44")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AD").dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some("24AD"),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AD").periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AD").periodToDate()),
          maybeChargeReference = Some(chargeReferenceGen.sample.get)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("-4577.44"),
          maybeOutstandingAmount = Some(BigDecimal("-2577.44")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AC").dueDate(),
          transactionType = TransactionType.Return,
          maybePeriodKey = Some("24AC"),
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AC").periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AC").periodToDate()),
          maybeChargeReference = Some(chargeReferenceGen.sample.get)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("20.56"),
          maybeOutstandingAmount = Some(BigDecimal("20.56")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodFromDate(),
          transactionType = TransactionType.LPI,
          maybePeriodKey = None,
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodToDate()),
          maybeChargeReference = Some(chargeReferenceGen.sample.get)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("20.56"),
          maybeOutstandingAmount = Some(BigDecimal("10.56")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodFromDate(),
          transactionType = TransactionType.LPI,
          maybePeriodKey = None,
          maybeTaxPeriodFrom = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodFromDate()),
          maybeTaxPeriodTo = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AB").periodToDate()),
          maybeChargeReference = Some(chargeReferenceGen.sample.get)
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("-1000.00"),
          maybeOutstandingAmount = Some(BigDecimal("-1000.00")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AH").periodFromDate(),
          transactionType = TransactionType.Overpayment,
          maybePeriodKey = None,
          maybeTaxPeriodFrom = None,
          maybeTaxPeriodTo = None,
          maybeChargeReference = None
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("-500.00"),
          maybeOutstandingAmount = Some(BigDecimal("-500.00")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AH").periodFromDate(),
          transactionType = TransactionType.Overpayment,
          maybePeriodKey = None,
          maybeTaxPeriodFrom = None,
          maybeTaxPeriodTo = None,
          maybeChargeReference = None
        ),
        createFinancialDocument(
          onlyOpenItems = onlyOpenItems,
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          originalAmount = BigDecimal("-50.00"),
          maybeOutstandingAmount = Some(BigDecimal("-50.00")),
          dueDate = ReturnPeriod.fromPeriodKeyOrThrow("24AA").periodFromDate(),
          transactionType = TransactionType.RPI,
          maybePeriodKey = None,
          maybeTaxPeriodFrom = None,
          maybeTaxPeriodTo = None,
          maybeChargeReference = None
        ),
        nilReturnLineItemsCancelling
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
    ),
    contactPreference = Digital,
    emailBounced = false
  )

  val adrObligationDetails = new AdrObligationData(
    status = ObligationStatus.Open,
    fromDate = LocalDate.of(2024, 1, 1),
    toDate = LocalDate.of(2024, 1, 1),
    dueDate = LocalDate.of(2024, 1, 1),
    periodKey
  )

  val adrObligationDetailsOpen2 = new AdrObligationData(
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
    Seq(adrObligationDetails, adrObligationDetailsOpen2, adrObligationDetails.copy(periodKey = periodKey3))

  val adrMultipleFulfilledData =
    Seq(adrObligationDetailsFulfilled, adrObligationDetailsFulfilled.copy(periodKey = periodKey2))

  def fulfilledObligation(yearMonth: YearMonth): AdrObligationData =
    AdrObligationData(
      status = ObligationStatus.Fulfilled,
      fromDate = yearMonth.atDay(1),
      toDate = yearMonth.atEndOfMonth(),
      dueDate = yearMonth.plusMonths(1).atDay(15),
      periodKey = ReturnPeriod(yearMonth).toPeriodKey
    )

  val fulfilledObligations2025 = FulfilledObligations(
    year = 2025,
    obligations = Seq(
      fulfilledObligation(YearMonth.of(2025, Month.APRIL)),
      fulfilledObligation(YearMonth.of(2025, Month.MARCH)),
      fulfilledObligation(YearMonth.of(2025, Month.FEBRUARY))
    )
  )

  val fulfilledObligations2024 = FulfilledObligations(
    year = 2025,
    obligations = Seq(
      fulfilledObligation(YearMonth.of(2024, Month.FEBRUARY)),
      fulfilledObligation(YearMonth.of(2024, Month.JANUARY))
    )
  )

  val fulfilledObligations2023 = FulfilledObligations(2023, Seq.empty)

  val fulfilledObligationsData = Seq(fulfilledObligations2023, fulfilledObligations2024, fulfilledObligations2025)

  val userFulfilledObligations = UserFulfilledObligations(appaId, fulfilledObligationsData, Instant.now(clock))

  val noOpenPayments: OpenPayments = OpenPayments(
    outstandingPayments = Seq.empty,
    totalOutstandingPayments = BigDecimal(0),
    unallocatedPayments = Seq.empty,
    totalUnallocatedPayments = BigDecimal(0),
    totalOpenPaymentsAmount = BigDecimal(0)
  )

  val chargeReference = chargeReferenceGen.sample.get

  def historicPayment(yearMonth: YearMonth, transactionType: TransactionType, amount: BigDecimal): HistoricPayment =
    HistoricPayment(
      period = ReturnPeriod(yearMonth),
      taxPeriodFrom = ReturnPeriod(yearMonth).periodFromDate(),
      taxPeriodTo = ReturnPeriod(yearMonth).periodToDate(),
      transactionType = transactionType,
      chargeReference = Some(chargeReference),
      amountPaid = amount
    )

  val historicReturnPayment = historicPayment(YearMonth.of(2025, Month.APRIL), Return, BigDecimal(1236.45))
  val historicLPIPayment    = historicPayment(YearMonth.of(2025, Month.JUNE), LPI, BigDecimal(12.45))
  val historicCAPayment     = historicPayment(YearMonth.of(2025, Month.MAY), CA, BigDecimal(234.45))
  val historicCAIPayment    = historicPayment(YearMonth.of(2025, Month.JULY), Return, BigDecimal(123.45))

  val historicPayments2025 =
    HistoricPayments(2025, Seq(historicReturnPayment, historicLPIPayment, historicCAPayment, historicCAIPayment))
  val historicPayments2024 = HistoricPayments(
    2024,
    Seq(
      historicPayment(YearMonth.of(2024, Month.DECEMBER), Return, BigDecimal(123.45)),
      historicPayment(YearMonth.of(2024, Month.NOVEMBER), LPI, BigDecimal(12.45))
    )
  )
  val historicPayments2023 = HistoricPayments(2023, Seq.empty)
  val historicPayments2022 = HistoricPayments(
    2022,
    Seq(historicPayment(YearMonth.of(2022, Month.DECEMBER), Return, BigDecimal(123.45)))
  )

  val historicPaymentsData = Seq(historicPayments2022, historicPayments2023, historicPayments2024, historicPayments2025)

  val userHistoricPayments = UserHistoricPayments(appaId, historicPaymentsData, Instant.now(clock))

  case class DownstreamErrorDetails(code: String, message: String, logID: String)

  object DownstreamErrorDetails {
    implicit val downstreamErrorDetailsWrites: OFormat[DownstreamErrorDetails] = Json.format[DownstreamErrorDetails]
  }

  val badRequest          = DownstreamErrorDetails("400", "You messed up", "id")
  val unprocessable       = DownstreamErrorDetails("422", "Unprocessable", "id")
  val internalServerError = DownstreamErrorDetails("500", "Computer says No!", "id")
  val badGateway          = DownstreamErrorDetails("502", "Bad gateway", "id")
  val serviceUnavailable  = DownstreamErrorDetails("503", "Service unavailable", "id")
  val gatewayTimeout      = DownstreamErrorDetails("504", "Gateway timeout", "id")
}

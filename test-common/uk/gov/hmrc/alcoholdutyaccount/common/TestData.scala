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
import uk.gov.hmrc.alcoholdutyaccount.models.{AdrObligationData, ObligationStatus, ReturnPeriod}
import uk.gov.hmrc.alcoholdutyaccount.models.hods._
import uk.gov.hmrc.alcoholdutyaccount.models.payments.{HistoricPayments, OpenPayments, TransactionType}
import uk.gov.hmrc.alcoholdutyaccount.models.subscription.{AdrSubscriptionSummary, AlcoholRegime, ApprovalStatus}

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait TestData extends ModelGenerators {
  val clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of("UTC"))

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val appaId: String          = appaIdGen.sample.get
  val businessPartner: String = businessPartnerGen.sample.get
  val contractAccount: String = contractAccountGen.sample.get
  val contractObject: String  = contractObjectGen.sample.get

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

  val financialTransaction = FinancialTransaction(
    sapDocumentNumber = "123456",
    periodKey = Some("18AA"),
    chargeReference = Some("X1234567890"),
    originalAmount = 1000.00,
    outstandingAmount = Some(50.00),
    mainTransaction = "1001",
    subTransaction = "1111",
    items = Seq(
      FinancialTransactionItem(
        subItem = "001",
        dueDate = None,
        amount = 50.00
      )
    )
  )

  val emptyFinancialDocument = FinancialTransactionDocument(financialTransactions = Seq.empty)

  val financialDocumentWithSingleSapDocumentNo = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        sapDocumentNumber = "123456",
        periodKey = Some("18AA"),
        chargeReference = Some("X1234567890"),
        originalAmount = 1000.00,
        outstandingAmount = Some(50.00),
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
        chargeReference = Some("X1234567890"),
        originalAmount = 1000.00,
        outstandingAmount = Some(50.00),
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

  val financialDocumentWithMultipleSapDocumentNumbers = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        sapDocumentNumber = "123456",
        periodKey = Some("18AA"),
        chargeReference = Some("X1234567890"),
        originalAmount = 1000.00,
        outstandingAmount = Some(50.00),
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
        sapDocumentNumber = "123457",
        periodKey = Some("18AA"),
        chargeReference = Some("X1234567891"),
        originalAmount = 1000.00,
        outstandingAmount = Some(50.00),
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

  val financialDocumentMinimal = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        sapDocumentNumber = "123456",
        periodKey = None,
        chargeReference = None,
        originalAmount = 1000.00,
        outstandingAmount = Some(50.00),
        mainTransaction = "1001",
        subTransaction = "2222",
        items = Seq(
          FinancialTransactionItem(
            subItem = "001",
            dueDate = None,
            amount = 1000.00
          )
        )
      )
    )
  )

  val singleFullyOutstandingReturn: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("9000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("9000")
            )
          )
        )
      )
    )

  def singlePartiallyOutstandingReturn(open: Boolean): FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("5000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("5000")
            ),
            FinancialTransactionItem(
              subItem = "001",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("4000")
            )
          )
        ).withOpen(open)
      )
    )

  val singlePaidReturn: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = None,
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("9000")
            )
          )
        )
      )
    )

  def twoLineItemPartiallyOutstandingReturn(open: Boolean): FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumber,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReference),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("5000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("5000")
            ),
            FinancialTransactionItem(
              subItem = "001",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("4000")
            )
          )
        ).withOpen(open),
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumber,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReference),
          originalAmount = BigDecimal("2000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("2000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("2000")
            )
          )
        )
      )
    )
  }

  // No outstanding amounts as line items cancel
  val nilReturnLineItemsCancelling: FinancialTransactionDocument = {
    val sapDocumentNumber = sapDocumentNumberGen.sample.get
    val chargeReference   = chargeReferenceGen.sample.get
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumber,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReference),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = None,
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("9000")
            )
          )
        ),
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumber,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReference),
          originalAmount = BigDecimal("-9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = None,
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("-9000")
            )
          )
        )
      )
    )
  }

  def twoSeparateOutstandingReturnsOnePartiallyPaid(open: Boolean): FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("5000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("5000")
            ),
            FinancialTransactionItem(
              subItem = "001",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("4000")
            )
          )
        ).withOpen(open),
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey2),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("2000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("2000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate()),
              amount = BigDecimal("2000")
            )
          )
        )
      )
    )

  val twoSeparateReturnsOneFullyPaid: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = None,
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("9000")
            )
          )
        ),
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = Some(periodKey2),
          chargeReference = Some(chargeReferenceGen.sample.get),
          originalAmount = BigDecimal("2000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("2000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey2).dueDate()),
              amount = BigDecimal("2000")
            )
          )
        )
      )
    )

  val singlePaymentOnAccount: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = None,
          chargeReference = None,
          originalAmount = BigDecimal("-9000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.PaymentOnAccount),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("-9000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
              amount = BigDecimal("-9000")
            )
          )
        )
      )
    )

  val twoSeparatePaymentsOnAccount: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = None,
          chargeReference = None,
          originalAmount = BigDecimal("-5000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.PaymentOnAccount),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("-5000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("-5000")
            )
          )
        ),
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = None,
          chargeReference = None,
          originalAmount = BigDecimal("-2000"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.PaymentOnAccount),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("-2000")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).dueDate()),
              amount = BigDecimal("-2000")
            )
          )
        )
      )
    )

  val singleFullyOutstandingLPI: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = None,
          chargeReference = chargeReferenceGen.sample,
          originalAmount = BigDecimal("50"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.LPI),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("50")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
              amount = BigDecimal("50")
            )
          )
        )
      )
    )

  val singleRPI: FinancialTransactionDocument =
    FinancialTransactionDocument(
      financialTransactions = Seq(
        FinancialTransaction(
          sapDocumentNumber = sapDocumentNumberGen.sample.get,
          periodKey = None,
          chargeReference = chargeReferenceGen.sample,
          originalAmount = BigDecimal("-50"),
          mainTransaction = TransactionType.toMainTransactionType(TransactionType.RPI),
          subTransaction = "6132",
          outstandingAmount = Some(BigDecimal("-50")),
          items = Seq(
            FinancialTransactionItem(
              subItem = "000",
              dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow(periodKey).periodFromDate()),
              amount = BigDecimal("-50")
            )
          )
        )
      )
    )

  def multipleStatuses(open: Boolean): FinancialTransactionDocument = FinancialTransactionDocument(
    financialTransactions = Seq(
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = Some("24AH"),
        chargeReference = Some(chargeReferenceGen.sample.get),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
        subTransaction = "6132",
        originalAmount = BigDecimal("237.44"),
        outstandingAmount = Some(BigDecimal("237.44")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 9, 25)),
            amount = BigDecimal("237.4")
          )
        )
      ),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = Some("24AE"),
        chargeReference = Some(chargeReferenceGen.sample.get),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
        subTransaction = "6132",
        originalAmount = BigDecimal("4577.44"),
        outstandingAmount = Some(BigDecimal("4577.44")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 6, 25)),
            amount = BigDecimal("4577.44")
          )
        )
      ),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = Some("24AD"),
        chargeReference = Some(chargeReferenceGen.sample.get),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
        subTransaction = "6132",
        originalAmount = BigDecimal("4577.44"),
        outstandingAmount = Some(BigDecimal("2577.44")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 5, 25)),
            amount = BigDecimal("2577.44")
          ),
          FinancialTransactionItem(
            subItem = "001",
            dueDate = Some(LocalDate.of(2024, 5, 25)),
            amount = BigDecimal("2000.00")
          )
        )
      ).withOpen(open),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = Some("24AC"),
        chargeReference = Some(chargeReferenceGen.sample.get),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.Return),
        subTransaction = "6132",
        originalAmount = BigDecimal("-4577.44"),
        outstandingAmount = Some(BigDecimal("-2577.44")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 4, 25)),
            amount = BigDecimal("-2577.44")
          ),
          FinancialTransactionItem(
            subItem = "001",
            dueDate = Some(LocalDate.of(2024, 4, 25)),
            amount = BigDecimal("-2000.00")
          )
        )
      ).withOpen(open),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = None,
        chargeReference = Some(chargeReferenceGen.sample.get),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.LPI),
        subTransaction = "6132",
        originalAmount = BigDecimal("20.56"),
        outstandingAmount = Some(BigDecimal("20.56")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 2, 1)),
            amount = BigDecimal("20.56")
          )
        )
      ),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = None,
        chargeReference = Some(chargeReferenceGen.sample.get),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.LPI),
        subTransaction = "6132",
        originalAmount = BigDecimal("20.56"),
        outstandingAmount = Some(BigDecimal("10.56")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 2, 1)),
            amount = BigDecimal("10.56")
          ),
          FinancialTransactionItem(
            subItem = "001",
            dueDate = Some(LocalDate.of(2024, 2, 1)),
            amount = BigDecimal("10.00")
          )
        )
      ).withOpen(open),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = None,
        chargeReference = None,
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.PaymentOnAccount),
        subTransaction = "6132",
        originalAmount = BigDecimal("-1000.00"),
        outstandingAmount = Some(BigDecimal("-1000.00")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 8, 1)),
            amount = BigDecimal("-1000.00")
          )
        )
      ),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = None,
        chargeReference = None,
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.PaymentOnAccount),
        subTransaction = "6132",
        originalAmount = BigDecimal("-500.00"),
        outstandingAmount = Some(BigDecimal("-500.00")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(LocalDate.of(2024, 8, 1)),
            amount = BigDecimal("-500.00")
          )
        )
      ),
      FinancialTransaction(
        sapDocumentNumber = sapDocumentNumberGen.sample.get,
        periodKey = None,
        chargeReference = None,
        originalAmount = BigDecimal("-50"),
        mainTransaction = TransactionType.toMainTransactionType(TransactionType.RPI),
        subTransaction = "6132",
        outstandingAmount = Some(BigDecimal("-50")),
        items = Seq(
          FinancialTransactionItem(
            subItem = "000",
            dueDate = Some(ReturnPeriod.fromPeriodKeyOrThrow("24AA").periodFromDate()),
            amount = BigDecimal("-50")
          )
        )
      )
    ) ++ nilReturnLineItemsCancelling.financialTransactions
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
    Seq(adrObligationDetails, adrObligationDetailsOpen2)

  val noOpenPayments: OpenPayments = OpenPayments(
    outstandingPayments = Seq.empty,
    totalOutstandingPayments = BigDecimal(0),
    unallocatedPayments = Seq.empty,
    totalUnallocatedPayments = BigDecimal(0),
    totalOpenPaymentsAmount = BigDecimal(0)
  )

  val noHistoricPayments: HistoricPayments = HistoricPayments(
    year = 2024,
    payments = Seq.empty
  )

  case class DownstreamErrorDetails(code: String, message: String, logID: String)

  object DownstreamErrorDetails {
    implicit val downstreamErrorDetailsWrites: OFormat[DownstreamErrorDetails] = Json.format[DownstreamErrorDetails]
  }

  val badRequest          = DownstreamErrorDetails("400", "You messed up", "id")
  val internalServerError = DownstreamErrorDetails("500", "Computer says No!", "id")
}

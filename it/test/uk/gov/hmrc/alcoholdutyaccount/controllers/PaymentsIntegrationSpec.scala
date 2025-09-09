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

package uk.gov.hmrc.alcoholdutyaccount.controllers

import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, ISpecBase}
import uk.gov.hmrc.alcoholdutyaccount.repositories.UserHistoricPaymentsRepository
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.Clock

class PaymentsIntegrationSpec extends ISpecBase with ConnectorTestHelpers {
  protected val endpointName = "financial"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .configure(Map("mongodb.uri" -> "mongodb://localhost:27017/test-alcohol-duty-account"))
      .overrides(bind(classOf[Clock]).toInstance(clock2025))
      .build()

  lazy val repository = app.injector.instanceOf[UserHistoricPaymentsRepository]

  override def beforeEach(): Unit = {
    await(repository.deleteAll())
    super.beforeEach()
  }
  override def afterEach(): Unit = {
    await(repository.deleteAll())
    super.afterEach()
  }

  "the open payments endpoint must" - {
    "respond with OK if able to fetch open payments" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, openParameters, OK, financialDataStubJson())

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)                 mustBe OK
      contentAsJson(response).toString mustBe openPayments

      verifyGetWithParameters(url, openParameters)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, openParameters, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))

      verifyGetWithParameters(url, openParameters)
    }

    "respond with an empty document if financial data not found for appaId" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, openParameters, NOT_FOUND, "")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)                 mustBe OK
      contentAsJson(response).toString mustBe noOpenPayments

      verifyGetWithParameters(url, openParameters)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the financial data api call" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, openParameters, INTERNAL_SERVER_ERROR, "")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))

      verifyGetWithParameters(url, openParameters)
    }
  }

  "the historic payments endpoint must" - {
    "respond with OK if able to fetch historic payments from the cache" in new SetUp {
      await(repository.set(userHistoricPayments))

      stubAuthorised(appaId)

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.historicPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(historicPaymentsData)

      verifyGetWithParametersNeverCalled(url, historicParameters2024)
      verifyGetWithParametersNeverCalled(url, historicParameters2025)
    }

    "respond with OK if able to fetch historic payments from the connector" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, historicParameters2024, OK, financialDataStubJson(year - 1))
      stubGetWithParameters(url, historicParameters2025, OK, financialDataStubJson(year))

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.historicPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)                 mustBe OK
      contentAsJson(response).toString mustBe historicPayments

      verifyGetWithParameters(url, historicParameters2024)
      verifyGetWithParameters(url, historicParameters2025)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved from the connector cannot be parsed" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, historicParameters2024, OK, financialDataStubJson(year - 1))
      stubGetWithParameters(url, historicParameters2025, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.historicPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))

      verifyGetWithParameters(url, historicParameters2024)
      verifyGetWithParameters(url, historicParameters2025)
    }

    "respond with an empty document if financial data not found for appaId" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, historicParameters2024, NOT_FOUND, "")
      stubGetWithParameters(url, historicParameters2025, NOT_FOUND, "")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.historicPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)                 mustBe OK
      contentAsJson(response).toString mustBe noHistoricPayments

      verifyGetWithParameters(url, historicParameters2024)
      verifyGetWithParameters(url, historicParameters2025)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the financial data api call" in new SetUp {
      stubAuthorised(appaId)
      stubGetWithParameters(url, historicParameters2024, INTERNAL_SERVER_ERROR, "")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.historicPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(response) mustBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))

      verifyGetWithParameters(url, historicParameters2024)
      verifyGetWithParametersNeverCalled(url, historicParameters2025)
    }
  }

  class SetUp {
    val url = config.financialDataUrl(appaId)

    val year = 2025

    val openParameters = Seq(
      "onlyOpenItems"              -> true.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString
    )

    val historicParameters2025 = Seq(
      "onlyOpenItems"              -> false.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString,
      "dateFrom"                   -> "2025-01-01",
      "dateTo"                     -> "2025-12-31"
    )

    val historicParameters2024 = Seq(
      "onlyOpenItems"              -> false.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString,
      "dateFrom"                   -> "2024-01-01",
      "dateTo"                     -> "2024-12-31"
    )

    def financialDataStubJson(year: Int = year) = {
      val yr = year.toString.takeRight(2)
      s"""{
        |  "idType": "ZAD",
        |  "idNumber": "$appaId",
        |  "regimeType": "AD",
        |  "processingDate": "$year-08-21T13:44:32Z",
        |  "financialTransactions": [
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "${yr}AH",
        |      "periodKeyDescription": "August $year",
        |      "taxPeriodFrom": "$year-08-01",
        |      "taxPeriodTo": "$year-08-31",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "550567384343",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA91104208683855",
        |      "mainTransaction": "6074",
        |      "subTransaction": "6132",
        |      "originalAmount": 237.44,
        |      "outstandingAmount": 237.44,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-09-25",
        |          "amount": 237.44
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "${yr}AE",
        |      "periodKeyDescription": "May $year",
        |      "taxPeriodFrom": "$year-05-01",
        |      "taxPeriodTo": "$year-05-31",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "658867377545",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA95767883826728",
        |      "mainTransaction": "6074",
        |      "subTransaction": "6132",
        |      "originalAmount": 4577.44,
        |      "outstandingAmount": 4577.44,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-06-25",
        |          "amount": 4577.44
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "${yr}AD",
        |      "periodKeyDescription": "April $year",
        |      "taxPeriodFrom": "$year-04-01",
        |      "taxPeriodTo": "$year-04-30",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "360333633872",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA07406454540955",
        |      "mainTransaction": "6074",
        |      "subTransaction": "6132",
        |      "originalAmount": 4577.44,
        |      "outstandingAmount": 2577.44,
        |      "clearedAmount": 2000.00,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-05-25",
        |          "amount": 2577.44
        |        },
        |        {
        |          "subItem": "001",
        |          "dueDate": "$year-05-25",
        |          "amount": 2000.00,
        |          "clearingDate": "$year-05-25",
        |          "clearingReason": "Incoming Payment",
        |          "clearingSAPDocument": "930266216883"
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "${yr}AC",
        |      "periodKeyDescription": "March $year",
        |      "taxPeriodFrom": "$year-03-01",
        |      "taxPeriodTo": "$year-03-31",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "469120358255",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA15775952652650",
        |      "mainTransaction": "6074",
        |      "subTransaction": "6132",
        |      "originalAmount": -4577.44,
        |      "outstandingAmount": -2577.44,
        |      "clearedAmount": -2000.00,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-04-25",
        |          "amount": -2577.44
        |        },
        |        {
        |          "subItem": "001",
        |          "dueDate": "$year-04-25",
        |          "amount": -2000.00,
        |          "clearingDate": "$year-04-25",
        |          "clearingReason": "Incoming Payment",
        |          "clearingSAPDocument": "280900265898"
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es int",
        |      "mainType": "Alcohol Duty Interest",
        |      "taxPeriodFrom": "$year-02-01",
        |      "taxPeriodTo": "$year-02-29",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "811594066000",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA85353805192234",
        |      "mainTransaction": "6075",
        |      "subTransaction": "6132",
        |      "originalAmount": 20.56,
        |      "outstandingAmount": 20.56,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-02-01",
        |          "amount": 20.56
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es int",
        |      "mainType": "Alcohol Duty Interest",
        |      "taxPeriodFrom": "$year-02-01",
        |      "taxPeriodTo": "$year-02-29",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "764278200842",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA63139412020838",
        |      "mainTransaction": "6075",
        |      "subTransaction": "6132",
        |      "originalAmount": 20.56,
        |      "outstandingAmount": 10.56,
        |      "clearedAmount": 10.00,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-02-01",
        |          "amount": 10.56
        |        },
        |        {
        |          "subItem": "001",
        |          "dueDate": "$year-02-01",
        |          "amount": 10.00,
        |          "clearingDate": "$year-02-01",
        |          "clearingReason": "Incoming Payment",
        |          "clearingSAPDocument": "744695668171"
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "Overpayment",
        |      "mainType": "On account",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "102204878592",
        |      "sapDocumentNumberItem": "0001",
        |      "mainTransaction": "0060",
        |      "subTransaction": "6132",
        |      "originalAmount": -1000,
        |      "outstandingAmount": -1000,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-08-01",
        |          "amount": -1000
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "Overpayment",
        |      "mainType": "On account",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "238469730497",
        |      "sapDocumentNumberItem": "0001",
        |      "mainTransaction": "0060",
        |      "subTransaction": "6132",
        |      "originalAmount": -500,
        |      "outstandingAmount": -500,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-08-01",
        |          "amount": -500
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "Alcohol Duty Repayment Int",
        |      "mainType": "Alcohol Duty Repayment Int",
        |      "businessPartner": "$businessPartner",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "$contractAccount",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "$contractObject",
        |      "sapDocumentNumber": "899485548080",
        |      "sapDocumentNumberItem": "0001",
        |      "chargeReference": "XA69201353871649",
        |      "mainTransaction": "6076",
        |      "subTransaction": "6132",
        |      "originalAmount": -50,
        |      "outstandingAmount": -50,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "$year-03-01",
        |          "amount": -50
        |        }
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin
    }

    val openPayments       =
      s"""{"outstandingPayments":[{"transactionType":"LPI","dueDate":"$year-02-01","chargeReference":"XA85353805192234","remainingAmount":20.56},{"transactionType":"Return","dueDate":"$year-09-25","chargeReference":"XA91104208683855","remainingAmount":237.44},{"transactionType":"Return","dueDate":"$year-06-25","chargeReference":"XA95767883826728","remainingAmount":4577.44},{"transactionType":"Return","dueDate":"$year-05-25","chargeReference":"XA07406454540955","remainingAmount":2577.44},{"transactionType":"RPI","dueDate":"$year-03-01","chargeReference":"XA69201353871649","remainingAmount":-50},{"transactionType":"Return","dueDate":"$year-04-25","chargeReference":"XA15775952652650","remainingAmount":-2577.44},{"transactionType":"LPI","dueDate":"$year-02-01","chargeReference":"XA63139412020838","remainingAmount":10.56}],"totalOutstandingPayments":4796,"unallocatedPayments":[{"paymentDate":"$year-08-01","unallocatedAmount":-1000},{"paymentDate":"$year-08-01","unallocatedAmount":-500}],"totalUnallocatedPayments":-1500,"totalOpenPaymentsAmount":3296}"""
    val historicPayments   =
      s"""[{"year":${year - 1},"payments":[{"period":"24AB","transactionType":"LPI","chargeReference":"XA63139412020838","amountPaid":10},{"period":"24AD","transactionType":"Return","chargeReference":"XA07406454540955","amountPaid":2000}]},{"year":$year,"payments":[{"period":"25AB","transactionType":"LPI","chargeReference":"XA63139412020838","amountPaid":10},{"period":"25AD","transactionType":"Return","chargeReference":"XA07406454540955","amountPaid":2000}]}]"""
    val noOpenPayments     =
      """{"outstandingPayments":[],"totalOutstandingPayments":0,"unallocatedPayments":[],"totalUnallocatedPayments":0,"totalOpenPaymentsAmount":0}"""
    val noHistoricPayments = s"""[{"year":${year - 1},"payments":[]},{"year":$year,"payments":[]}]"""
  }
}

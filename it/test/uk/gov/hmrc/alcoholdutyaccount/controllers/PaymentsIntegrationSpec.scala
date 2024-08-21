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

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.alcoholdutyaccount.base.{ConnectorTestHelpers, ISpecBase}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class PaymentsIntegrationSpec extends ISpecBase with ConnectorTestHelpers {
  protected val endpointName = "financial"

  "the open payments endpoint should" should {
    "respond with OK if able to fetch open payments" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, parameters, OK, financialDataStubJson)

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe OK
      contentAsJson(response).toString shouldBe openPayments

      verifyGetWithParameters(url, parameters)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, parameters, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))

      verifyGetWithParameters(url, parameters)
    }

    "respond with NOT_FOUND if financial data not found for appaId" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, parameters, NOT_FOUND, "")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe NOT_FOUND
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Entity not found"))

      verifyGetWithParameters(url, parameters)
    }

    "respond with INTERNAL_SERVER_ERROR if error(s) returned from the financial data api call" in new SetUp {
      stubAuthorised()
      stubGetWithParameters(url, parameters, INTERNAL_SERVER_ERROR, "")

      val response = callRoute(
        FakeRequest("GET", routes.PaymentsController.openPayments(appaId).url)
          .withHeaders("Authorization" -> "Bearer 12345")
      )

      status(response) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(response) shouldBe Json.toJson(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected Response"))

      verifyGetWithParameters(url, parameters)
    }
  }

  class SetUp {
    val url       = config.financialDataUrl(appaId)

    val parameters =     Seq(
      "onlyOpenItems"              -> true.toString,
      "includeLocks"               -> false.toString,
      "calculateAccruedInterest"   -> false.toString,
      "customerPaymentInformation" -> false.toString
    )

    val financialDataStubJson =
      """{
        |  "idType": "ZAD",
        |  "idNumber": "XMADP1000800208",
        |  "regimeType": "AD",
        |  "processingDate": "2024-08-21T13:44:32Z",
        |  "financialTransactions": [
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "24AH",
        |      "periodKeyDescription": "August 2024",
        |      "taxPeriodFrom": "2024-08-01",
        |      "taxPeriodTo": "2024-08-31",
        |      "businessPartner": "1546568303",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "906405799233",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "42414154478355120115",
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
        |          "dueDate": "2024-09-25",
        |          "amount": 237.44
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "24AE",
        |      "periodKeyDescription": "May 2024",
        |      "taxPeriodFrom": "2024-05-01",
        |      "taxPeriodTo": "2024-05-31",
        |      "businessPartner": "4179880455",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "975250625993",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "06068730056890811379",
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
        |          "dueDate": "2024-06-25",
        |          "amount": 4577.44
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "24AD",
        |      "periodKeyDescription": "April 2024",
        |      "taxPeriodFrom": "2024-04-01",
        |      "taxPeriodTo": "2024-04-30",
        |      "businessPartner": "0037678789",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "033917587512",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "06615040603837274548",
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
        |          "dueDate": "2024-05-25",
        |          "amount": 2577.44
        |        },
        |        {
        |          "subItem": "001",
        |          "dueDate": "2024-05-25",
        |          "amount": 2000.00,
        |          "clearingDate": "2024-05-25",
        |          "clearingReason": "Incoming Payment",
        |          "clearingSAPDocument": "930266216883"
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es",
        |      "mainType": "Alcohol Duty Return",
        |      "periodKey": "24AC",
        |      "periodKeyDescription": "March 2024",
        |      "taxPeriodFrom": "2024-03-01",
        |      "taxPeriodTo": "2024-03-31",
        |      "businessPartner": "5007891678",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "668608926277",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "45542278005978646601",
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
        |          "dueDate": "2024-04-25",
        |          "amount": -2577.44
        |        },
        |        {
        |          "subItem": "001",
        |          "dueDate": "2024-04-25",
        |          "amount": -2000.00,
        |          "clearingDate": "2024-04-25",
        |          "clearingReason": "Incoming Payment",
        |          "clearingSAPDocument": "280900265898"
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es int",
        |      "mainType": "Alcohol Duty Interest",
        |      "taxPeriodFrom": "2024-02-01",
        |      "taxPeriodTo": "2024-02-29",
        |      "businessPartner": "8337950470",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "512316438709",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "62952989772245219667",
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
        |          "dueDate": "2024-02-01",
        |          "amount": 20.56
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "SP Beer 1.3-3.4 es int",
        |      "mainType": "Alcohol Duty Interest",
        |      "taxPeriodFrom": "2024-02-01",
        |      "taxPeriodTo": "2024-02-29",
        |      "businessPartner": "2315843349",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "986963317689",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "04194429852574946140",
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
        |          "dueDate": "2024-02-01",
        |          "amount": 10.56
        |        },
        |        {
        |          "subItem": "001",
        |          "dueDate": "2024-02-01",
        |          "amount": 10.00,
        |          "clearingDate": "2024-02-01",
        |          "clearingReason": "Incoming Payment",
        |          "clearingSAPDocument": "744695668171"
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "Payment on account",
        |      "mainType": "On account",
        |      "businessPartner": "3900513878",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "907879960526",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "26746239066398397458",
        |      "sapDocumentNumber": "102204878592",
        |      "sapDocumentNumberItem": "0001",
        |      "mainTransaction": "0060",
        |      "subTransaction": "6132",
        |      "originalAmount": -1000,
        |      "outstandingAmount": -1000,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "2024-08-01",
        |          "amount": -1000
        |        }
        |      ]
        |    },
        |    {
        |      "chargeType": "Payment on account",
        |      "mainType": "On account",
        |      "businessPartner": "7424508742",
        |      "contractAccountCategory": "51",
        |      "contractAccount": "082655887559",
        |      "contractObjectType": "ZADP",
        |      "contractObject": "50459162893080807444",
        |      "sapDocumentNumber": "238469730497",
        |      "sapDocumentNumberItem": "0001",
        |      "mainTransaction": "0060",
        |      "subTransaction": "6132",
        |      "originalAmount": -500,
        |      "outstandingAmount": -500,
        |      "items": [
        |        {
        |          "subItem": "000",
        |          "dueDate": "2024-08-01",
        |          "amount": -500
        |        }
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin

    val openPayments = """{"outstandingPayments":[{"transactionType":"LPI","dueDate":"2024-02-01","chargeReference":"XA85353805192234","remainingAmount":20.56},{"transactionType":"Return","dueDate":"2024-09-25","chargeReference":"XA91104208683855","remainingAmount":237.44},{"transactionType":"Return","dueDate":"2024-06-25","chargeReference":"XA95767883826728","remainingAmount":4577.44},{"transactionType":"Return","dueDate":"2024-05-25","chargeReference":"XA07406454540955","remainingAmount":2577.44},{"transactionType":"Return","dueDate":"2024-04-25","chargeReference":"XA15775952652650","remainingAmount":-2577.44},{"transactionType":"LPI","dueDate":"2024-02-01","chargeReference":"XA63139412020838","remainingAmount":10.56}],"totalOutstandingPayments":4846,"unallocatedPayments":[{"paymentDate":"2024-08-01","unallocatedAmount":-1000},{"paymentDate":"2024-08-01","unallocatedAmount":-500}],"totalUnallocatedPayments":-1500,"totalOpenPaymentsAmount":3346}"""
  }
}

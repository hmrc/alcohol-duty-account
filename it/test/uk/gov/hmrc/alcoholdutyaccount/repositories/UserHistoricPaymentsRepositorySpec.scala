/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.alcoholdutyaccount.repositories

import org.mongodb.scala.model.Filters
import org.scalatest.Assertion
import uk.gov.hmrc.alcoholdutyaccount.base.ISpecBase
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.payments.UserHistoricPayments
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit

class UserHistoricPaymentsRepositorySpec
    extends ISpecBase
    with DefaultPlayMongoRepositorySupport[UserHistoricPayments] {

  private val DB_TTL_IN_SEC = 100

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.dbTimeToLiveInSeconds) thenReturn DB_TTL_IN_SEC

  protected override val repository = new UserHistoricPaymentsRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig
  )

  "get must" - {
    "get the record for the appaId when it exists" in {
      insert(userHistoricPayments).futureValue

      val result = repository.get(appaId).futureValue
      verifyResult(result.value, userHistoricPayments)
    }

    "return None when there is no record for the appaId" in {
      repository.get("APPA id that does not exist").futureValue must not be defined
    }
  }

  "set must" - {
    "replace the record for the given appaId" in {
      insert(userHistoricPayments.copy(historicPaymentsData = Seq(historicPayments2025))).futureValue

      val setResult   = repository.set(userHistoricPayments).futureValue
      val addedRecord = find(Filters.equal("_id", appaId)).futureValue.headOption.value

      setResult mustEqual userHistoricPayments
      verifyResult(addedRecord, userHistoricPayments)
    }

    "add the record if no record for the appaId exists" in {
      val setResult   = repository.set(userHistoricPayments).futureValue
      val addedRecord = find(Filters.equal("_id", appaId)).futureValue.headOption.value

      setResult mustEqual userHistoricPayments
      verifyResult(addedRecord, userHistoricPayments)
    }
  }

  "deleteAll must" - {
    "delete all records from the collection" in {
      insert(userHistoricPayments).futureValue
      insert(userHistoricPayments.copy(appaId = "test")).futureValue
      val initialRecords = findAll().futureValue

      repository.deleteAll().futureValue
      val finalRecords = findAll().futureValue

      initialRecords.length mustEqual 2
      finalRecords.length   mustEqual 0
    }
  }

  def verifyResult(actual: UserHistoricPayments, expected: UserHistoricPayments): Assertion = {
    actual.appaId                                   mustEqual expected.appaId
    actual.historicPaymentsData                     mustEqual expected.historicPaymentsData
    actual.createdAt.truncatedTo(ChronoUnit.MILLIS) mustEqual expected.createdAt.truncatedTo(ChronoUnit.MILLIS)
  }
}

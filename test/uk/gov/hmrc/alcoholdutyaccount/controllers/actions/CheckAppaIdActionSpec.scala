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

package uk.gov.hmrc.alcoholdutyaccount.controllers.actions

import play.api.mvc.Result
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.models.requests.IdentifierRequest

import scala.concurrent.Future

class CheckAppaIdActionSpec extends SpecBase {
  val wrongAppaId           = appaId + "1"
  val fakeIdentifierRequest = IdentifierRequest(fakeRequest, appaId)
  val testContent           = "Test"

  val testAction: IdentifierRequest[_] => Future[Result] = { request =>
    request mustBe fakeIdentifierRequest

    Future(Ok(testContent))
  }

  "CheckAppaIdAction" - {
    "must succeed if appaId matches that in the enrolment" in {
      val checkAppaIdAction = new CheckAppaIdAction
      val result            = checkAppaIdAction(appaId).invokeBlock(fakeIdentifierRequest, testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent
    }

    "must fail if appaId doesn't match that in the enrolment" in {
      val checkAppaIdAction = new CheckAppaIdAction
      val result            = checkAppaIdAction(wrongAppaId).invokeBlock(fakeIdentifierRequest, testAction)

      status(result) mustBe UNAUTHORIZED
    }
  }
}

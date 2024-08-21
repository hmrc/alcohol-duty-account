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

package uk.gov.hmrc.alcoholdutyaccount.models

import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class ErrorCodesSpec extends SpecBase {
  "sanitiseError" - {
    "should sanitise BAD_REQUEST to a standard error" in {
      ErrorCodes.sanitiseError(ErrorResponse(BAD_REQUEST, "Bad boy!")) mustBe ErrorCodes.badRequest
    }

    "should sanitise NOT_FOUND to a standard error" in {
      ErrorCodes.sanitiseError(ErrorResponse(NOT_FOUND, "Couldn't find it!")) mustBe ErrorCodes.entityNotFound
    }

    "should sanitise UNPROCESSABLE_ENTITY to a standard error" in {
      ErrorCodes.sanitiseError(ErrorResponse(UNPROCESSABLE_ENTITY, "Couldn't read it!")) mustBe ErrorCodes.invalidJson
    }

    "should sanitise anything else to an INTERNAL_SERVER_ERROR" in {
      ErrorCodes.sanitiseError(ErrorResponse(IM_A_TEAPOT, "Oh yes I am!")) mustBe ErrorCodes.unexpectedResponse
    }
  }
}

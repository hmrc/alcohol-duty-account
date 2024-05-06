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

class ReturnPeriodSpec extends SpecBase {
  "ReturnPeriod" - {
    "should return a ReturnPeriod when fromPeriodKey is called with a valid period key" in {
      val validPeriodKey = "24AA"
      val result         = ReturnPeriod.fromPeriodKey(validPeriodKey)

      result mustBe Some(ReturnPeriod(validPeriodKey))
    }

    "should return None from when fromPeriodKey is called with an invalid period key" in {
      val validPeriodKey = "24AA"
      val result         = ReturnPeriod.fromPeriodKey(validPeriodKey)

      result mustBe Some(ReturnPeriod(validPeriodKey))
    }
  }
}

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

package uk.gov.hmrc.alcoholdutyaccount.models.subscription

import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.{JsString, Writes}

sealed trait ContactPreferenceForBTA extends EnumEntry

object ContactPreferenceForBTA extends Enum[ContactPreferenceForBTA] {
  val values = findValues

  case object Digital extends ContactPreferenceForBTA
  case object Paper extends ContactPreferenceForBTA

  implicit val writes: Writes[ContactPreferenceForBTA] = {
    case Digital => JsString("digital")
    case Paper   => JsString("paper")
  }
}

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

package uk.gov.hmrc.alcoholdutyaccount.models.subscription

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait AlcoholRegime extends EnumEntry

object AlcoholRegime extends Enum[AlcoholRegime] with PlayJsonEnum[AlcoholRegime] {
  val values = findValues

  case object Beer extends AlcoholRegime
  case object Cider extends AlcoholRegime
  case object Wine extends AlcoholRegime
  case object Spirits extends AlcoholRegime
  case object OtherFermentedProduct extends AlcoholRegime
}

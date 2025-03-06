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

package uk.gov.hmrc.alcoholdutyaccount.utils

import uk.gov.hmrc.alcoholdutyaccount.config.Constants.ukTimeZoneStringId

import java.time.{Instant, LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone

object DateTimeHelper {
  private val ukTimeZone: ZoneId = TimeZone.getTimeZone(ukTimeZoneStringId).toZoneId

  def instantToLocalDate(instant: Instant): LocalDate = LocalDate.ofInstant(instant, ukTimeZone)

  def formatISOInstantSeconds(now: Instant): String =
    DateTimeFormatter.ISO_INSTANT.format(now.truncatedTo(ChronoUnit.SECONDS))
}

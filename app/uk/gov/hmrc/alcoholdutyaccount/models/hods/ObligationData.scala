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

package uk.gov.hmrc.alcoholdutyaccount.models.hods

import play.api.libs.json._

import java.time.LocalDate

case class ObligationData(
  obligations: Seq[Obligation]
)

object ObligationData {
  implicit val reads: Reads[ObligationData] = Json.reads[ObligationData]
}

sealed trait ObligationStatus
case object Open extends ObligationStatus
case object Fulfilled extends ObligationStatus

object ObligationStatus {
  implicit val jsonReads: Reads[ObligationStatus] = (json: JsValue) =>
    json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "O" => JsSuccess(Open)
          case "F" => JsSuccess(Fulfilled)
          case s   => JsError(s"$s is not a valid ObligationStatus")
        }
      case e: JsError          => e
    }
}

final case class ObligationDetails(
  status: ObligationStatus,
  inboundCorrespondenceFromDate: LocalDate,
  inboundCorrespondenceToDate: LocalDate,
  inboundCorrespondenceDateReceived: Option[LocalDate],
  inboundCorrespondenceDueDate: LocalDate,
  periodKey: String
)

object ObligationDetails {
  implicit val reads: Reads[ObligationDetails] = Json.reads[ObligationDetails]
}

final case class Obligation(
  obligationDetails: Seq[ObligationDetails]
)
object Obligation {
  implicit val reads: Reads[Obligation] = Json.reads[Obligation]
}

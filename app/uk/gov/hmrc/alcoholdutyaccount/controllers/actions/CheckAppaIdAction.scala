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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.alcoholdutyaccount.models.ErrorCodes
import uk.gov.hmrc.alcoholdutyaccount.models.requests.IdentifierRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckAppaIdActionImpl private[actions] (appaId: String)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[IdentifierRequest, IdentifierRequest]
    with Logging {
  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] =
    Future.successful(checkRequestedAppaId(request.appaId, appaId).map(_ => request))

  private def checkRequestedAppaId(identifiedAppaId: String, appaIdToCheck: String): Either[Result, Unit] =
    if (appaIdToCheck != identifiedAppaId) {
      logger.error(
        s"Manual call of endpoint or bug (using unauthorised appaId): Endpoint appaId requested $appaIdToCheck, enrolment appaId was $identifiedAppaId"
      )
      Left(
        Unauthorized(
          Json.toJson(ErrorCodes.unauthorisedRequest)
        )
      )
    } else {
      Right(())
    }
}

class CheckAppaIdAction @Inject() (implicit val executionContext: ExecutionContext) {
  def apply(appaId: String): ActionRefiner[IdentifierRequest, IdentifierRequest] =
    new CheckAppaIdActionImpl(appaId)(executionContext)
}

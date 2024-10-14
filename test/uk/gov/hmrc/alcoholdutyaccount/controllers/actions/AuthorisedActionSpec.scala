/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.mvc.{BodyParsers, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.alcoholdutyaccount.base.SpecBase
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.requests.IdentifierRequest
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.CredentialStrength.strong
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.authorisedEnrolments

import scala.concurrent.Future

class AuthorisedActionSpec extends SpecBase {
  val enrolment               = "HMRC-AD-ORG"
  val appaIdKey               = "APPAID"
  val state                   = "Activated"
  val enrolments              = Enrolments(Set(Enrolment(enrolment, Seq(EnrolmentIdentifier(appaIdKey, appaId)), state)))
  val emptyEnrolments         = Enrolments(Set.empty)
  val enrolmentsWithoutAppaId = Enrolments(Set(Enrolment(enrolment, Seq.empty, state)))
  val testContent             = "Test"

  override val appConfig                     = mock[AppConfig]
  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val authorisedAction =
    new BaseAuthorisedAction(mockAuthConnector, appConfig, defaultBodyParser)

  when(appConfig.enrolmentServiceName).thenReturn(enrolment)
  when(appConfig.enrolmentIdentifierKey).thenReturn(appaIdKey)

  val testAction: IdentifierRequest[_] => Future[Result] = { request =>
    request.appaId mustBe appaId

    Future(Ok(testContent))
  }

  "invokeBlock" - {
    "should execute the block and return OK if authorised" in {
      when(
        mockAuthConnector.authorise(
          eqTo(
            AuthProviders(GovernmentGateway)
              and Enrolment(enrolment)
              and CredentialStrength(strong)
              and Organisation
              and ConfidenceLevel.L50
          ),
          eqTo(authorisedEnrolments)
        )(any(), any())
      )
        .thenReturn(Future.successful(enrolments))

      val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

      status(result) mustBe OK
      contentAsString(result) mustBe testContent
    }

    "should execute the block and throw IllegalStateException if cannot get the enrolment" in {
      when(
        mockAuthConnector.authorise(
          eqTo(
            AuthProviders(GovernmentGateway)
              and Enrolment(enrolment)
              and CredentialStrength(strong)
              and Organisation
              and ConfidenceLevel.L50
          ),
          eqTo(authorisedEnrolments)
        )(any(), any())
      )
        .thenReturn(Future.successful(emptyEnrolments))

      intercept[IllegalStateException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }
    }

    "should execute the block and throw IllegalStateException if cannot get the APPAID enrolment" in {
      when(
        mockAuthConnector.authorise(
          eqTo(
            AuthProviders(GovernmentGateway)
              and Enrolment(enrolment)
              and CredentialStrength(strong)
              and Organisation
              and ConfidenceLevel.L50
          ),
          eqTo(authorisedEnrolments)
        )(any(), any())
      )
        .thenReturn(Future.successful(emptyEnrolments))

      intercept[IllegalStateException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }
    }

    "should return 401 Unauthorized if there is an authorisation exception" - {
      List(
        InsufficientConfidenceLevel(),
        InsufficientEnrolments(),
        UnsupportedAffinityGroup(),
        UnsupportedCredentialRole(),
        UnsupportedAuthProvider(),
        IncorrectCredentialStrength(),
        InternalError(),
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result) mustBe UNAUTHORIZED
      }
    }

    "should return the exception if there is any other exception" in {
      val msg = "Test Exception"

      when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Test Exception")))

      val result = intercept[RuntimeException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage mustBe msg
    }
  }
}

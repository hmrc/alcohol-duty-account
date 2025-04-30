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

package uk.gov.hmrc.alcoholdutyaccount.config

import com.google.inject.{Inject, Provider, Singleton}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import play.api.Configuration

import scala.concurrent.ExecutionContext

@Singleton
class CircuitBreakerProvider @Inject() (config: Configuration)(implicit
  ec: ExecutionContext,
  sys: ActorSystem,
  appConfig: AppConfig
) extends Provider[CircuitBreaker] {

  private val maxFailures  = appConfig.maxFailures
  private val callTimeout  = appConfig.callTimeout
  private val resetTimeout = appConfig.resetTimeout

  override def get(): CircuitBreaker =
    new CircuitBreaker(
      scheduler = sys.scheduler,
      maxFailures = maxFailures,
      callTimeout = callTimeout,
      resetTimeout = resetTimeout
    )
}

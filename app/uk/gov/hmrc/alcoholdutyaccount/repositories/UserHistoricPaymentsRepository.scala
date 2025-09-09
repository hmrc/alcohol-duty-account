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

package uk.gov.hmrc.alcoholdutyaccount.repositories

import org.mongodb.scala.model._
import uk.gov.hmrc.alcoholdutyaccount.config.AppConfig
import uk.gov.hmrc.alcoholdutyaccount.models.payments.UserHistoricPayments
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserHistoricPaymentsRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UserHistoricPayments](
      collectionName = "user-historic-payments",
      mongoComponent = mongoComponent,
      domainFormat = UserHistoricPayments.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("createdAt"),
          IndexOptions()
            .name("createdAtIdx")
            .expireAfter(appConfig.dbTimeToLiveInSeconds, TimeUnit.SECONDS)
        )
      ),
      extraCodecs = Seq.empty,
      replaceIndexes = true
    ) {

  private def byId(appaId: String) = Filters.equal("_id", appaId)

  def get(appaId: String): Future[Option[UserHistoricPayments]] =
    collection
      .find(byId(appaId))
      .headOption()

  def set(userHistoricPayments: UserHistoricPayments): Future[UserHistoricPayments] =
    collection
      .replaceOne(
        filter = byId(userHistoricPayments.appaId),
        replacement = userHistoricPayments,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => userHistoricPayments)

  def deleteAll(): Future[Unit] =
    collection.deleteMany(Filters.empty()).toFuture().map(_ => ())
}

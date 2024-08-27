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

package uk.gov.hmrc.alcoholdutyaccount.common.generators

import org.scalacheck.Gen

trait ModelGenerators {

  def periodKeyGen: Gen[String] = for {
    year  <- Gen.chooseNum(23, 50)
    month <- Gen.chooseNum(0, 11)
  } yield s"${year}A${(month + 'A').toChar}"

  def appaIdGen: Gen[String] = Gen.listOfN(10, Gen.numChar).map(id => s"XMADP${id.mkString}")

  def businessGen: Gen[String] = Gen.listOfN(6, Gen.alphaLowerChar).map(id => s"test$id Ltd")

  def chargeReferenceGen: Gen[String]   = Gen.listOfN(14, Gen.numChar).map(id => s"XA${id.mkString}")
  def businessPartnerGen: Gen[String]   = Gen.listOfN(10, Gen.numChar).map(_.mkString)
  def contractAccountGen: Gen[String]   = Gen.listOfN(12, Gen.numChar).map(_.mkString)
  def contractObjectGen: Gen[String]    = Gen.listOfN(20, Gen.numChar).map(_.mkString)
  def sapDocumentNumberGen: Gen[String] = Gen.listOfN(12, Gen.numChar).map(_.mkString)
}

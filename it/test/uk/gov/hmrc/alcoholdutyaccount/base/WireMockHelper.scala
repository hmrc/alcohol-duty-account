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

package uk.gov.hmrc.alcoholdutyaccount.base

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, get, okJson, post, put, removeStub, stubFor, urlEqualTo, urlMatching}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping


object WireMockHelper {

  val wireMockPort = 9999
  val wireMockHost = "localhost"
  val url          = s"http://$wireMockHost:$wireMockPort"

  def setWireMockPort(services: String*): Map[String, Any] =
    services.foldLeft(Map.empty[String, Any]) { case (map, service) =>
      map + (s"microservice.services.$service.port" -> wireMockPort)
    }

  def stub(method: MappingBuilder, response: ResponseDefinitionBuilder): StubMapping =
    stubFor(method.willReturn(response))

  def stubGet(uri: String, responseBody: String): StubMapping =
    stub(get(urlEqualTo(uri)), okJson(responseBody))

  def stubPost(
                url: String,
                responseStatus: Int,
                responseBody: String,
                responseHeader: (String, String) = ("", "")
              ): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      post(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        )
    )
  }

  def stubDelete(
                  url: String,
                  responseStatus: Int,
                  responseBody: String,
                  responseHeader: (String, String) = ("", "")
                ): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      delete(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        )
    )
  }

  def stubPut(
               url: String,
               responseStatus: Int,
               responseBody: String,
               responseHeader: (String, String) = ("", "")
             ): StubMapping = {
    removeStub(put(urlMatching(url)))
    stubFor(
      put(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        )
    )
  }

}

trait WireMockHelper {

  import WireMockHelper._

  lazy val wireMockConfiguration: WireMockConfiguration = wireMockConfig().port(wireMockPort)
  lazy val wireMockServer                               = new WireMockServer(wireMockConfiguration)

  def startWireMock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  def stopWireMock(): Unit = wireMockServer.stop()

  def resetWireMock(): Unit = WireMock.reset()

}


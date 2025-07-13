/*
 * Copyright (c) 2016-2023.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.gatehill.imposter.plugin.openapi.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.plugin.openapi.model.ContentTypedHolder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Tests for [ResponseTransmissionServiceImpl].
 *
 * @author Pete Cornish
 */
class ResponseTransmissionServiceImplTest {
    private val service = ResponseTransmissionServiceImpl()

    private fun createMockHttpExchange(): HttpExchange {
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { absoluteUri } doReturn "/test"
        }
        val httpResponse = mock<HttpResponse> {
            on { putHeader(any(), any()) } doReturn mock
            on { statusCode } doReturn 200
        }
        return mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
    }

    @Test
    fun `should serialise as JSON for application json`() {
        val httpExchange = createMockHttpExchange()
        val testData = mapOf("key" to "value", "number" to 42)
        val example = ContentTypedHolder("application/json", testData)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "application/json")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's JSON by checking it contains expected elements
        assertTrue(response.contains("\"key\""))
        assertTrue(response.contains("\"value\""))
        assertTrue(response.contains("\"number\""))
        assertTrue(response.contains("42"))
    }

    @Test
    fun `should serialise as JSON for structured syntax suffix json`() {
        val httpExchange = createMockHttpExchange()
        val testData = mapOf("key" to "value")
        val example = ContentTypedHolder("application/vnd.api+json", testData)

        service.transmitExample(httpExchange, example)

        verify(httpExchange.response).putHeader("Content-Type", "application/vnd.api+json")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's JSON format
        assertTrue(response.contains("\"key\""))
        assertTrue(response.contains("\"value\""))
    }

    @Test
    fun `should serialise as JSON for github issue example content type`() {
        val httpExchange = createMockHttpExchange()
        val testData = mapOf("test" to "data")
        val example = ContentTypedHolder("application/vnd.my.super.type.v1+json", testData)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "application/vnd.my.super.type.v1+json")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's JSON format
        assertTrue(response.contains("\"test\""))
        assertTrue(response.contains("\"data\""))
    }

    @Test
    fun `should serialise as YAML for YAML content types`() {
        val httpExchange = createMockHttpExchange()
        val testData = mapOf("key" to "value")
        val example = ContentTypedHolder("application/yaml", testData)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "application/yaml")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's YAML format (starts with --- and contains key: "value")
        assertTrue(response.contains("---"))
        assertTrue(response.contains("key: \"value\""))
    }

    @Test
    fun `should handle unsupported content types`() {
        val httpExchange = createMockHttpExchange()
        val testData = mapOf("key" to "value")
        val example = ContentTypedHolder("text/plain", testData)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "text/plain")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's toString format (contains = but not quotes)
        assertTrue(response.contains("key=value"))
        assertFalse(response.contains("\""))
    }

    @Test
    fun `should handle null example value`() {
        val httpExchange = createMockHttpExchange()
        val example = ContentTypedHolder("application/json", null)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).end()
    }

    @Test
    fun `should handle string examples`() {
        val httpExchange = createMockHttpExchange()
        val example = ContentTypedHolder("application/json", "test string")
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "application/json")
        verify(httpExchange.response).end("test string")
    }

    @Test
    fun `should handle list examples with JSON content type`() {
        val httpExchange = createMockHttpExchange()
        val testList = listOf(mapOf("id" to 1), mapOf("id" to 2))
        val example = ContentTypedHolder("application/vnd.api+json", testList)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "application/vnd.api+json")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's JSON array format
        assertTrue(response.contains("["))
        assertTrue(response.contains("]"))
        assertTrue(response.contains("\"id\""))
        assertTrue(response.contains("1"))
        assertTrue(response.contains("2"))
    }

    @Test
    fun `should not serialise as JSON for non-JSON content types`() {
        val httpExchange = createMockHttpExchange()
        val testData = mapOf("key" to "value")
        val example = ContentTypedHolder("application/xml", testData)
        
        service.transmitExample(httpExchange, example)
        
        verify(httpExchange.response).putHeader("Content-Type", "application/xml")
        
        val responseCaptor = argumentCaptor<String>()
        verify(httpExchange.response).end(responseCaptor.capture())
        val response = responseCaptor.firstValue
        
        // Verify it's NOT JSON (toString format)
        assertTrue(response.contains("key=value"))
        assertFalse(response.contains("\"key\""))
    }
}
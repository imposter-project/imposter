package io.gatehill.imposter.plugin.openapi

import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for OpenAPI custom content type
 */
class CustomContentTypeTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/openapi3/custom-content-type"
    )

    /**
     * Should return correct response with custom MIME type.
     */
    @Test
    fun testCustomContentType() {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(CONTENT_TYPE)
            .`when`().get("/pets/1")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(CONTENT_TYPE)
            .body("id", Matchers.equalTo(1))
            .body("name", Matchers.equalTo("Cat"))
    }

    companion object {
        private val CONTENT_TYPE = ContentType.fromContentType("application/vnd.pets.v1+json")
    }
}

package de.gruetzmann.movie100

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testSearchWithoutApiKey() = testApplication {
        application {
            module()
        }
        val response = client.get("/search")
        assertEquals(HttpStatusCode.FailedDependency, response.status)
    }
}
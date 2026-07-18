package com.alongside.data.pairing

import com.alongside.core.model.SyncStatus
import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.data.testTrip
import com.alongside.data.trip.TripFirestoreMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

// runBlocking, not runTest: runTest's virtual-time scheduler falsely times out
// Ktor HttpTimeout against MockEngine (see the M3 note in docs/roadmap.md).
class FirestorePairingRemoteDataSourceTest {
    private lateinit var capturedBody: String

    private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData =
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )

    private fun dataSource(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        FirestorePairingRemoteDataSource(
            FirestoreApi(
                HttpClient(
                    MockEngine { request ->
                        capturedBody = (request.body as TextContent).text
                        handler(request)
                    },
                ) { configureFirestoreHttpClient() },
                FirestoreConfig(projectId = "alongside-test"),
                FirestoreTokenProvider { null },
            ),
        )

    private fun tripDocumentJson(): String {
        val trip =
            testTrip(
                id = "trip-1",
                memberId = "member-1",
                createdAt = Instant.parse("2026-07-01T10:00:00Z"),
                updatedAt = Instant.parse("2026-07-18T12:30:00Z"),
            )
        val fields =
            com.alongside.core.network.firestore.model.firestoreJson
                .encodeToString(TripFirestoreMapper.toFields(trip))
        val name = "projects/p/databases/(default)/documents/trips/trip-1"
        return """[{"document": {"name": "$name", "fields": $fields}}]"""
    }

    @Test
    fun `findTripByInviteCode sends an EQUAL filter on inviteCode limited to one result`() {
        val dataSource = dataSource { respondJson("[]") }

        runBlocking { dataSource.findTripByInviteCode("ABCD23") }

        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "structuredQuery": {
                    "from": [{"collectionId": "trips"}],
                    "where": {
                      "fieldFilter": {
                        "field": {"fieldPath": "inviteCode"},
                        "op": "EQUAL",
                        "value": {"stringValue": "ABCD23"}
                      }
                    },
                    "limit": 1
                  }
                }
                """.trimIndent(),
            ),
            Json.parseToJsonElement(capturedBody),
        )
    }

    @Test
    fun `findTripByUserId sends an OR of ownerId and memberId equality filters`() {
        val dataSource = dataSource { respondJson("[]") }

        runBlocking { dataSource.findTripByUserId("uid-1") }

        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "structuredQuery": {
                    "from": [{"collectionId": "trips"}],
                    "where": {
                      "compositeFilter": {
                        "op": "OR",
                        "filters": [
                          {"fieldFilter": {"field": {"fieldPath": "ownerId"}, "op": "EQUAL",
                           "value": {"stringValue": "uid-1"}}},
                          {"fieldFilter": {"field": {"fieldPath": "memberId"}, "op": "EQUAL",
                           "value": {"stringValue": "uid-1"}}}
                        ]
                      }
                    },
                    "limit": 1
                  }
                }
                """.trimIndent(),
            ),
            Json.parseToJsonElement(capturedBody),
        )
    }

    @Test
    fun `a matched document is mapped to a SYNCED domain trip`() {
        val dataSource = dataSource { respondJson(tripDocumentJson()) }

        val trip = runBlocking { dataSource.findTripByInviteCode("ABCD23") }

        assertEquals("trip-1", trip?.id)
        assertEquals("member-1", trip?.memberId)
        assertEquals(SyncStatus.SYNCED, trip?.syncStatus)
        assertEquals(Instant.parse("2026-07-18T12:30:00Z"), trip?.updatedAt)
    }

    @Test
    fun `no match maps to null`() {
        val dataSource = dataSource { respondJson("""[{"readTime": "2026-07-18T00:00:00Z"}]""") }

        assertNull(runBlocking { dataSource.findTripByInviteCode("XXXX99") })
    }
}

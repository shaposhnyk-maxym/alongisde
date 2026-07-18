package com.alongside.core.network.firestore

import com.alongside.core.network.firestore.model.CollectionSelector
import com.alongside.core.network.firestore.model.FieldFilter
import com.alongside.core.network.firestore.model.FieldReference
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.QueryFilter
import com.alongside.core.network.firestore.model.StructuredQuery
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// runBlocking, not runTest: runTest's virtual-time scheduler falsely times out
// Ktor HttpTimeout against MockEngine (see the M3 note in docs/roadmap.md).
class FirestoreApiQueryTest {
    private fun inviteCodeQuery() =
        StructuredQuery(
            from = listOf(CollectionSelector("trips")),
            where =
                QueryFilter(
                    fieldFilter =
                        FieldFilter(
                            field = FieldReference("inviteCode"),
                            op = "EQUAL",
                            value = FirestoreValue.StringValue("ABCD23"),
                        ),
                ),
            limit = 1,
        )

    private fun documentElementJson(id: String) =
        """
        {
          "document": {
            "name": "projects/alongside-test/databases/(default)/documents/trips/$id",
            "fields": {"ownerId": {"stringValue": "owner-1"}},
            "createTime": "2024-01-01T00:00:00Z",
            "updateTime": "2024-01-02T00:00:00Z"
          },
          "readTime": "2024-01-03T00:00:00Z"
        }
        """.trimIndent()

    @Test
    fun `runQuery posts the structured query to the documents runQuery endpoint`() {
        lateinit var captured: HttpRequestData
        val api =
            testFirestoreApi { request ->
                captured = request
                respondJson("[${documentElementJson("trip-1")}]")
            }

        runBlocking { api.runQuery(inviteCodeQuery()) }

        assertEquals(HttpMethod.Post, captured.method)
        assertEquals(
            "https://firestore.googleapis.com/v1/projects/alongside-test/databases/(default)/documents:runQuery",
            captured.url.toString(),
        )
        val body = (captured.body as TextContent).text
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
            Json.parseToJsonElement(body),
        )
    }

    @Test
    fun `runQuery returns the matched documents in response order`() {
        val api =
            testFirestoreApi {
                respondJson("[${documentElementJson("trip-1")},${documentElementJson("trip-2")}]")
            }

        val documents = runBlocking { api.runQuery(inviteCodeQuery()) }

        assertEquals(2, documents.size)
        assertEquals(
            "projects/alongside-test/databases/(default)/documents/trips/trip-1",
            documents.first().name,
        )
    }

    @Test
    fun `runQuery skips read-time-only elements when nothing matches`() {
        val api = testFirestoreApi { respondJson("""[{"readTime": "2024-01-03T00:00:00Z"}]""") }

        val documents = runBlocking { api.runQuery(inviteCodeQuery()) }

        assertEquals(emptyList(), documents)
    }

    @Test
    fun `runQuery maps a 4xx response to ClientError`() {
        val api =
            testFirestoreApi {
                respondJson(
                    """{"error": {"code": 403, "message": "denied", "status": "PERMISSION_DENIED"}}""",
                    HttpStatusCode.Forbidden,
                )
            }

        val exception =
            assertFailsWith<FirestoreException.ClientError> {
                runBlocking { api.runQuery(inviteCodeQuery()) }
            }
        assertEquals(403, exception.code)
    }

    @Test
    fun `runQuery maps a 5xx response to ServerError`() {
        val api =
            testFirestoreApi {
                respondJson(
                    """{"error": {"code": 503, "message": "unavailable", "status": "UNAVAILABLE"}}""",
                    HttpStatusCode.ServiceUnavailable,
                )
            }

        assertFailsWith<FirestoreException.ServerError> {
            runBlocking { api.runQuery(inviteCodeQuery()) }
        }
    }
}

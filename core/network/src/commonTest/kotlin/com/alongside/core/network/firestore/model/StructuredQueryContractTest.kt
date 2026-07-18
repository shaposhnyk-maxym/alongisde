package com.alongside.core.network.firestore.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StructuredQueryContractTest {
    @Test
    fun `field filter query encodes to the documented runQuery shape`() {
        val request =
            RunQueryRequest(
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
                ),
            )

        val encoded = firestoreJson.encodeToString(request)

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
            Json.parseToJsonElement(encoded),
        )
    }

    @Test
    fun `composite OR filter encodes nested field filters`() {
        val request =
            RunQueryRequest(
                StructuredQuery(
                    from = listOf(CollectionSelector("trips")),
                    where =
                        QueryFilter(
                            compositeFilter =
                                CompositeFilter(
                                    op = "OR",
                                    filters =
                                        listOf(
                                            QueryFilter(
                                                fieldFilter =
                                                    FieldFilter(
                                                        field = FieldReference("ownerId"),
                                                        op = "EQUAL",
                                                        value = FirestoreValue.StringValue("uid-1"),
                                                    ),
                                            ),
                                            QueryFilter(
                                                fieldFilter =
                                                    FieldFilter(
                                                        field = FieldReference("memberId"),
                                                        op = "EQUAL",
                                                        value = FirestoreValue.StringValue("uid-1"),
                                                    ),
                                            ),
                                        ),
                                ),
                        ),
                ),
            )

        val encoded = firestoreJson.encodeToString(request)

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
                    }
                  }
                }
                """.trimIndent(),
            ),
            Json.parseToJsonElement(encoded),
        )
    }

    @Test
    fun `response element with a document decodes document and readTime`() {
        val element =
            firestoreJson.decodeFromString<RunQueryResponseElement>(
                """
                {
                  "document": {
                    "name": "projects/p/databases/(default)/documents/trips/trip-1",
                    "fields": {"ownerId": {"stringValue": "owner-1"}}
                  },
                  "readTime": "2024-01-02T00:00:00.000000Z"
                }
                """.trimIndent(),
            )

        assertEquals(
            FirestoreValue.StringValue("owner-1"),
            element.document?.fields?.get("ownerId"),
        )
        assertEquals("2024-01-02T00:00:00.000000Z", element.readTime)
    }

    @Test
    fun `response element without a document decodes with a null document`() {
        val element =
            firestoreJson.decodeFromString<RunQueryResponseElement>(
                """{"readTime": "2024-01-02T00:00:00.000000Z", "done": true}""",
            )

        assertNull(element.document)
        assertEquals(true, element.done)
        assertTrue(element.readTime != null)
    }
}

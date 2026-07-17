package com.alongside.core.network.firestore.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirestoreDocumentContractTest {
    private val documentedDocumentJson =
        """
        {
          "name": "projects/alongside-app/databases/(default)/documents/trips/trip-1",
          "fields": {
            "ownerId": {"stringValue": "owner-1"},
            "memberCount": {"integerValue": "2"},
            "active": {"booleanValue": true}
          },
          "createTime": "2024-01-01T00:00:00.000000Z",
          "updateTime": "2024-01-02T00:00:00.000000Z"
        }
        """.trimIndent()

    @Test
    fun `decodes a literal Firestore Document JSON into FirestoreDocument`() {
        val document = firestoreJson.decodeFromString<FirestoreDocument>(documentedDocumentJson)

        assertEquals("projects/alongside-app/databases/(default)/documents/trips/trip-1", document.name)
        assertEquals(FirestoreValue.StringValue("owner-1"), document.fields["ownerId"])
        assertEquals(FirestoreValue.IntegerValue(2), document.fields["memberCount"])
        assertEquals(FirestoreValue.BooleanValue(true), document.fields["active"])
        assertEquals("2024-01-01T00:00:00.000000Z", document.createTime)
        assertEquals("2024-01-02T00:00:00.000000Z", document.updateTime)
    }

    @Test
    fun `encoding a FirestoreDocument's fields matches the documented wrapper shape`() {
        val request =
            FirestoreWriteRequest(
                fields =
                    mapOf(
                        "ownerId" to FirestoreValue.StringValue("owner-1"),
                        "memberCount" to FirestoreValue.IntegerValue(2),
                    ),
            )

        val encoded = firestoreJson.encodeToString(request)

        assertEquals(
            Json.parseToJsonElement(
                """{"fields":{"ownerId":{"stringValue":"owner-1"},"memberCount":{"integerValue":"2"}}}""",
            ),
            Json.parseToJsonElement(encoded),
        )
    }

    @Test
    fun `list response with nextPageToken present decodes both fields`() {
        val json =
            """{"documents":[$documentedDocumentJson],"nextPageToken":"page-2"}"""

        val response = firestoreJson.decodeFromString<FirestoreListDocumentsResponse>(json)

        assertEquals(1, response.documents.size)
        assertEquals("page-2", response.nextPageToken)
    }

    @Test
    fun `list response without nextPageToken means last page`() {
        val json = """{"documents":[$documentedDocumentJson]}"""

        val response = firestoreJson.decodeFromString<FirestoreListDocumentsResponse>(json)

        assertEquals(1, response.documents.size)
        assertNull(response.nextPageToken)
    }
}

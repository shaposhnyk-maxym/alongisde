package com.alongside.core.network.firestore.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreErrorResponseContractTest {
    @Test
    fun `decodes Google's standard API error envelope`() {
        val json =
            """
            {"error": {"code": 401, "message": "Request had invalid authentication credentials.", "status": "UNAUTHENTICATED"}}
            """.trimIndent()

        val response = firestoreJson.decodeFromString<FirestoreErrorResponse>(json)

        assertEquals(401, response.error.code)
        assertEquals("Request had invalid authentication credentials.", response.error.message)
        assertEquals("UNAUTHENTICATED", response.error.status)
    }
}

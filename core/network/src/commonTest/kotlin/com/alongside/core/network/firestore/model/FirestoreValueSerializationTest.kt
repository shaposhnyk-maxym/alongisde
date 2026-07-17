package com.alongside.core.network.firestore.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirestoreValueSerializationTest {
    private fun roundTrip(value: FirestoreValue): FirestoreValue {
        val encoded = firestoreJson.encodeToString(FirestoreValueSerializer, value)
        return firestoreJson.decodeFromString(FirestoreValueSerializer, encoded)
    }

    private fun encodedElement(value: FirestoreValue) =
        Json.parseToJsonElement(firestoreJson.encodeToString(FirestoreValueSerializer, value))

    @Test
    fun `string value round-trips as stringValue`() {
        val value = FirestoreValue.StringValue("hello")

        assertEquals(value, roundTrip(value))
        assertEquals(Json.parseToJsonElement("""{"stringValue":"hello"}"""), encodedElement(value))
    }

    @Test
    fun `integer value is encoded as a string not a JSON number`() {
        val value = FirestoreValue.IntegerValue(42)

        assertEquals(value, roundTrip(value))
        assertEquals(Json.parseToJsonElement("""{"integerValue":"42"}"""), encodedElement(value))
    }

    @Test
    fun `double value round-trips as doubleValue`() {
        val value = FirestoreValue.DoubleValue(1.5)

        assertEquals(value, roundTrip(value))
        assertEquals(Json.parseToJsonElement("""{"doubleValue":1.5}"""), encodedElement(value))
    }

    @Test
    fun `boolean value round-trips as booleanValue`() {
        val value = FirestoreValue.BooleanValue(true)

        assertEquals(value, roundTrip(value))
        assertEquals(Json.parseToJsonElement("""{"booleanValue":true}"""), encodedElement(value))
    }

    @Test
    fun `timestamp value round-trips an RFC3339 string unchanged`() {
        val value = FirestoreValue.TimestampValue("2024-01-01T00:00:00Z")

        assertEquals(value, roundTrip(value))
        assertEquals(
            Json.parseToJsonElement("""{"timestampValue":"2024-01-01T00:00:00Z"}"""),
            encodedElement(value),
        )
    }

    @Test
    fun `null value round-trips as nullValue`() {
        val value = FirestoreValue.NullValue

        assertEquals(value, roundTrip(value))
        assertEquals(Json.parseToJsonElement("""{"nullValue":null}"""), encodedElement(value))
    }

    @Test
    fun `nested mapValue round-trips`() {
        val value =
            FirestoreValue.MapValue(
                FirestoreMapValue(fields = mapOf("city" to FirestoreValue.StringValue("Kyiv"))),
            )

        assertEquals(value, roundTrip(value))
        assertEquals(
            Json.parseToJsonElement("""{"mapValue":{"fields":{"city":{"stringValue":"Kyiv"}}}}"""),
            encodedElement(value),
        )
    }

    @Test
    fun `arrayValue round-trips with heterogeneous element types`() {
        val value =
            FirestoreValue.ArrayValue(
                FirestoreArrayValue(
                    values = listOf(FirestoreValue.StringValue("a"), FirestoreValue.IntegerValue(1)),
                ),
            )

        assertEquals(value, roundTrip(value))
        assertEquals(
            Json.parseToJsonElement("""{"arrayValue":{"values":[{"stringValue":"a"},{"integerValue":"1"}]}}"""),
            encodedElement(value),
        )
    }

    @Test
    fun `decoding an unrecognized value object throws`() {
        assertFailsWith<SerializationException> {
            firestoreJson.decodeFromString(FirestoreValueSerializer, """{"bogusValue":"x"}""")
        }
    }
}

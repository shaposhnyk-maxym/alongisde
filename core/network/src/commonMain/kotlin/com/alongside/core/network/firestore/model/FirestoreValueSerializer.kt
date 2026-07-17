package com.alongside.core.network.firestore.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Firestore encodes a value's type as the single key present in its wrapper object (e.g.
 * `{"stringValue": "..."}`), not via a class discriminator field - a
 * [kotlinx.serialization.json.JsonContentPolymorphicSerializer] doesn't fit here because it can
 * only deserialize (its `serialize()` throws), so both directions are implemented manually.
 */
public object FirestoreValueSerializer : KSerializer<FirestoreValue> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: FirestoreValue,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("FirestoreValue can only be serialized as JSON")
        val element =
            when (value) {
                is FirestoreValue.StringValue -> buildJsonObject { put("stringValue", JsonPrimitive(value.value)) }
                is FirestoreValue.IntegerValue ->
                    buildJsonObject { put("integerValue", JsonPrimitive(value.value.toString())) }
                is FirestoreValue.DoubleValue -> buildJsonObject { put("doubleValue", JsonPrimitive(value.value)) }
                is FirestoreValue.BooleanValue -> buildJsonObject { put("booleanValue", JsonPrimitive(value.value)) }
                is FirestoreValue.TimestampValue ->
                    buildJsonObject { put("timestampValue", JsonPrimitive(value.value)) }
                FirestoreValue.NullValue -> buildJsonObject { put("nullValue", JsonNull) }
                is FirestoreValue.MapValue ->
                    buildJsonObject { put("mapValue", jsonEncoder.json.encodeToJsonElement(value.value)) }
                is FirestoreValue.ArrayValue ->
                    buildJsonObject { put("arrayValue", jsonEncoder.json.encodeToJsonElement(value.value)) }
            }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): FirestoreValue {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("FirestoreValue can only be deserialized from JSON")
        val json = jsonDecoder.json
        val obj =
            jsonDecoder.decodeJsonElement() as? JsonObject
                ?: throw SerializationException("Expected a Firestore value object")
        return when {
            "stringValue" in obj -> FirestoreValue.StringValue(obj.getValue("stringValue").jsonPrimitive.content)
            "integerValue" in obj ->
                FirestoreValue.IntegerValue(
                    obj
                        .getValue("integerValue")
                        .jsonPrimitive.content
                        .toLong(),
                )
            "doubleValue" in obj -> FirestoreValue.DoubleValue(obj.getValue("doubleValue").jsonPrimitive.double)
            "booleanValue" in obj -> FirestoreValue.BooleanValue(obj.getValue("booleanValue").jsonPrimitive.boolean)
            "timestampValue" in obj ->
                FirestoreValue.TimestampValue(obj.getValue("timestampValue").jsonPrimitive.content)
            "nullValue" in obj -> FirestoreValue.NullValue
            "mapValue" in obj -> FirestoreValue.MapValue(json.decodeFromJsonElement(obj.getValue("mapValue")))
            "arrayValue" in obj -> FirestoreValue.ArrayValue(json.decodeFromJsonElement(obj.getValue("arrayValue")))
            else -> throw SerializationException("Unrecognized Firestore value: $obj")
        }
    }
}

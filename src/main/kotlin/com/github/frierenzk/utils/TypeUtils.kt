package com.github.frierenzk.utils

import com.google.gson.*

object TypeUtils {
    inline fun <reified A, reified B> Pair<*, *>.isPairOf(): Boolean {
        return first is A && second is B
    }

    inline fun <reified A, reified B> Pair<*, *>.asPairOf(): Pair<A, B>? {
        if (!isPairOf<A, B>()) return null
        return first as A to second as B
    }

    inline fun <reified A, reified B> Map<*, *>.isMapOf(): Boolean {
        return all { it.key is A && it.value is B }
    }

    inline fun <reified A, reified B> Map<*, *>.asMapOf(): Map<A, B>? {
        return if (isMapOf<A, B>())
            @Suppress("UNCHECKED_CAST")
            this as Map<A, B>
        else null
    }

    fun castJsonPrimitive(jsonPrimitive: JsonPrimitive): Any {
        return when {
            jsonPrimitive.isBoolean -> jsonPrimitive.asBoolean
            jsonPrimitive.isNumber -> jsonPrimitive.asNumber
            jsonPrimitive.isString -> jsonPrimitive.asString
            else -> ""
        }
    }

    fun castIntoJsonObject(jsonElement: JsonElement): JsonObject {
        return jsonElement.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
    }

    fun castIntoJsonArray(jsonElement: JsonElement): JsonArray {
        return jsonElement.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
    }

    fun isJsonArrayOrObject(json: String): Boolean {
        return try {
            JsonParser.parseString(json).let {
                it.isJsonObject || it.isJsonArray
            }
        } catch (exception: JsonParseException) {
            false
        }
    }
}
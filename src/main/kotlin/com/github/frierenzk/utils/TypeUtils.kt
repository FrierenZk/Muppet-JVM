package com.github.frierenzk.utils

import com.google.gson.JsonParseException
import com.google.gson.JsonParser

object TypeUtils {
    inline fun <reified T1, reified T2> castPairs(args: Pair<*, *>): Pair<T1?, T2?> {
        val (key, value) = args
        return if (key is T1 && value is T2) Pair(key, value)
        else Pair(null, null)
    }

    inline fun <reified T1, reified T2> castMap(args: HashMap<*, *>): HashMap<T1, T2> {
        val dstMap = hashMapOf<T1, T2>()
        args.forEach { (key, value) ->
            if (key is T1 && value is T2) dstMap[key] = value
        }
        return dstMap
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
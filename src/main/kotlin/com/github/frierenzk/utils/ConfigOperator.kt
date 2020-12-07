package com.github.frierenzk.utils

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

object ConfigOperator {
    private fun loadFile(file: File): JsonElement {
        return try {
            if (file.exists())
                JsonParser.parseReader(file.reader())
            else {
                file.createNewFile()
                JsonNull.INSTANCE
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            JsonNull.INSTANCE
        }
    }

    fun loadServerConfig(): JsonObject {
        val jsonObject = loadFile(File("server_settings.json")).takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        if (!jsonObject.has("port")) jsonObject.addProperty("port", 21518)
        return jsonObject
    }

    fun loadTickerConfig(): HashMap<String, HashMap<String, String>> {
        val jsonObject = loadFile(File("timer.json")).takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        val ret = hashMapOf<String, HashMap<String, String>>()
        jsonObject.entrySet().forEach {
            (it.value.asJsonObject ?: JsonObject()).entrySet()?.forEach { (key, value) ->
                value.asString?.run { ret.getOrPut(it.key) { hashMapOf() }[key] = this }
            }
        }
        return HashMap(ret.filterNot { it.value.containsKey("interval") })
    }
}
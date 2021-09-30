package com.github.frierenzk.task

import com.google.gson.*
import java.lang.reflect.Type

class BuildConfigAdaptor : JsonSerializer<BuildConfig> {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    override fun serialize(src: BuildConfig?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return when (src) {
            is BuildConfig -> {
                try {
                    val jsonObject = gson.toJsonTree(src).asJsonObject
                    jsonObject.remove("projectDir")
                    jsonObject
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    JsonNull.INSTANCE
                }
            }
            else -> JsonNull.INSTANCE
        }
    }
}
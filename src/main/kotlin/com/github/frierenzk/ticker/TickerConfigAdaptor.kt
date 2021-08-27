package com.github.frierenzk.ticker

import com.google.gson.*
import java.lang.reflect.Type

class TickerConfigAdaptor : JsonSerializer<TickerConfig> {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    override fun serialize(src: TickerConfig?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return when (src) {
            is TickerConfig.Ref -> gson.toJsonTree(src, TickerConfig.Ref::class.java)
            is TickerConfig.Mix -> gson.toJsonTree(src, TickerConfig.Mix::class.java)
            is TickerConfig.Config -> gson.toJsonTree(src, TickerConfig.Config::class.java)
            else -> JsonNull.INSTANCE
        }
    }
}
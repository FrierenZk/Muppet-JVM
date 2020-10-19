package com.github.frierenzk.ticker

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileWriter

internal class TickerConfigGen {
    @Test
    fun gen() {
        val gson = GsonBuilder().setPrettyPrinting().create()!!
        val file = File("timer.json").apply { if (!this.exists()) this.createNewFile() }
        val list = listOf(hashMapOf(
                "name" to "wifi6",
                "interval" to 120
        ))
        if (file.canWrite()) {
            val writer = FileWriter(file)
            writer.write(gson.toJson(list))
            writer.flush()
            writer.close()
        }
    }
}
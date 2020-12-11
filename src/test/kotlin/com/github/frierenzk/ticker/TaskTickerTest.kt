package com.github.frierenzk.ticker

import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

internal class TaskTickerTest {
    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    @Test
    fun resetTest() {
        val gson = GsonBuilder().setPrettyPrinting().create()!!
        val file = File("timer.json").apply { if (!this.exists()) this.createNewFile() }
        val list = hashMapOf(
            "wifi6_new4" to hashMapOf(
                "interval" to 120,
                "buildOnlyIfUpdated" to true
            )
        )
        file.bufferedWriter().run {
            this.write(gson.toJson(list))
            this.flush()
            this.close()
        }
        val ticker = TaskTicker().apply { init() }
        runBlocking {
            ticker.sendEvent(TickerEvent.Reset, 0)
            delay(2000)
            assertEquals(true, ticker.raisedEvent.isEmpty)
        }
    }
}
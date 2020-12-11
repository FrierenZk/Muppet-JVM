package com.github.frierenzk.ticker

import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.TypeUtils
import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TaskTickerTest {
    @ExperimentalCoroutinesApi
    @Test
    @Order(1)
    fun resetTest() {
        val gson = GsonBuilder().setPrettyPrinting().create()!!
        val ticker = TaskTicker().apply { init() }
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
        runBlocking {
            ticker.sendEvent(TickerEvent.Reset, 0)
            delay(2000)
            assertEquals(true, ticker.raisedEvent.isEmpty)
        }
        file.delete()
        ticker.close()
    }


    @Test
    @Order(2)
    fun addTimerTest() = runBlocking {
        val ticker = TaskTicker().apply { init() }
        ticker.sendEvent(
            TickerEvent.AddTimer, hashMapOf(
                "name" to "wifi6_new4",
                "interval" to 120 as Number,
                "buildOnlyIfUpdated" to true
            )
        )
        delay(1000)
        assertEquals(false, ticker.tasks.isEmpty())
        ticker.close()
    }

    @Test
    @Order(3)
    fun enableTest() = runBlocking {
        val ticker = TaskTicker().apply { init() }
        ticker.sendEvent(TickerEvent.Disable, "wifi6_new4")
        delay(100)
        assertEquals(true, ticker.tasks.isEmpty())
        ticker.sendEvent(TickerEvent.Enable, "wifi6_new4")
        delay(100)
        assertEquals(false, ticker.tasks.isEmpty())
        ticker.close()
    }

    @Test
    @Order(4)
    fun modifyTest() = runBlocking {
        val ticker = TaskTicker().apply { init() }
        ticker.sendEvent(
            TickerEvent.ModifyInterval, hashMapOf(
                "name" to "wifi6_new4",
                "interval" to 0
            )
        )
        val minute = ticker(61 * 1000)
        val (event, args) = select<Pair<EventType, Any>> {
            minute.onReceive { Pair(TickerEvent.Default, "") }
            ticker.raisedEvent.onReceive { it }
        }
        assertEquals(PoolEvent.AddTask, event)
        val map = TypeUtils.castMap<String, Any>(args as HashMap<*, *>)
        assertEquals("wifi6_new4", map["name"])
        ticker.sendEvent(
            TickerEvent.ModifyInterval, hashMapOf(
                "name" to "wifi6_new4",
                "interval" to 120
            )
        )
        ticker.close()
    }
}
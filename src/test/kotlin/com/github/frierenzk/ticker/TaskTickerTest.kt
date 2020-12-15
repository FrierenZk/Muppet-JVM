package com.github.frierenzk.ticker

import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.TestUtils.waitingFor
import com.github.frierenzk.utils.TypeUtils.asMapOf
import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
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
    fun resetTest() = runBlocking {
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
        var string = ""
        val channel = Channel<Unit>()
        ticker.sendEvent(TickerEvent.Reset, Pipe.callback { it: String -> string = it; channel.sendBlocking(Unit) })
        waitingFor(channel, 3000)
        assertEquals(true, ticker.raisedEvent.isEmpty)
        assertEquals("Success", string)
        file.delete()
        ticker.close()
    }


    @Test
    @Order(2)
    fun addTimerTest() = runBlocking {
        val ticker = TaskTicker().apply { init() }
        var string = ""
        val channel = Channel<Unit>()
        val pipe = Pipe<Map<String, Any>, String>(
            hashMapOf(
                "name" to "wifi6_new4",
                "interval" to 120 as Number,
                "buildOnlyIfUpdated" to true
            )
        ) { string = it;channel.sendBlocking(Unit) }
        ticker.sendEvent(
            TickerEvent.AddTimer, pipe
        )
        waitingFor(channel, 1000)
        assertEquals(false, ticker.tasks.isEmpty())
        assertEquals("Success", string)
        ticker.close()
    }

    @Test
    @Order(3)
    fun enableTest() = runBlocking {
        val ticker = TaskTicker().apply { init() }
        var string = ""
        val channel = Channel<Unit>()
        ticker.sendEvent(
            TickerEvent.Disable,
            Pipe<String, String>("wifi6_new4") { string = it;channel.sendBlocking(Unit) })
        waitingFor(channel, 1000)
        assertEquals("Success", string)
        assertEquals(true, ticker.tasks.isEmpty())
        ticker.sendEvent(
            TickerEvent.Enable,
            Pipe<String, String>("wifi6_new4") { string = it;channel.sendBlocking(Unit) })
        waitingFor(channel, 1000)
        assertEquals("Success", string)
        assertEquals(false, ticker.tasks.isEmpty())
        ticker.close()
    }

    @Test
    @Order(4)
    @Suppress("UNCHECKED_CAST")
    fun modifyTest() = runBlocking {
        val ticker = TaskTicker().apply { init() }
        var string = ""
        ticker.sendEvent(
            TickerEvent.ModifyInterval, Pipe("wifi6_new4" to 0) { it: String -> string = it }
        )
        val (event, args) = waitingFor(ticker.raisedEvent, 61 * 1000) as Pair<EventType, Pipe<*, *>>
        assertEquals("Success", string)
        assertEquals(PoolEvent.AddTask, event)
        val map = (args.data as HashMap<*, *>).asMapOf<String, Any>()
        assertEquals("wifi6_new4", map?.get("name"))
        ticker.sendEvent(
            TickerEvent.ModifyInterval, Pipe("wifi6_new4" to 120) { it: String -> string = it }
        )
        assertEquals("Success", string)
        ticker.close()
    }
}
package com.github.frierenzk.server

import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.ticker.TickerEvent
import com.github.frierenzk.utils.TestUtils.waitingFor
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertEquals

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class LinkageTest {
    companion object {
        @JvmStatic
        private val linkage by lazy { Linkage().apply { init() } }
    }

    @Test
    @Order(1)
    fun getEventMonitor() {
        println(linkage.eventMonitor)
    }

    @Test
    @Order(2)
    fun receiveEvent() = runBlocking {
        linkage.sendEvent(ServerEvent.Default, Pipe.default)
    }

    @Suppress("UNCHECKED_CAST")
    @ExperimentalCoroutinesApi
    @Test
    @Order(3)
    fun listeners(): Unit = runBlocking {
        val client = IO.socket("http://127.0.0.1:21518")
        val connectChan = Channel<String>()
        client.on(Socket.EVENT_CONNECT) { println(Socket.EVENT_CONNECT).also { runBlocking { connectChan.send("Remote server connected") } } }
        client.connect()
        waitingFor(connectChan, 3000)
        connectChan.close()
        val testProject = hashMapOf(
            "set_add_task" to PoolEvent.AddTask,
            "set_stop_task" to PoolEvent.StopTask,
            "get_waiting_list" to PoolEvent.WaitingList,
            "get_processing_list" to PoolEvent.WorkingList,
            "get_available_list" to PoolEvent.AvailableList,
            "reload_config" to PoolEvent.ReloadConfig,
            "set_create_task" to PoolEvent.CreateTask,

            "reset_ticker" to TickerEvent.Reset,
            "enable_ticker" to TickerEvent.Enable,
            "disable_ticker" to TickerEvent.Disable,
            "add_timer" to TickerEvent.AddTimer,
            "modify_interval" to TickerEvent.ModifyInterval
        )
        testProject.forEach { (event, received) ->
            val ack = object : Ack {
                val channel by lazy { Channel<String>(1) }
                override fun call(vararg args: Any?) {
                    channel.sendBlocking(args.joinToString())
                }
            }
            println("Now testing event $event")
            client.emit(event, "test", ack)
            val reply = waitingFor(ack.channel, 100)
            println("reply = $reply")
            if (reply == "Time out") {
                val (rEvent, args) = waitingFor(linkage.raisedEvent, 3000) as Pair<EventType, Pipe<*, *>>
                assertEquals(received, rEvent)
                println("data = ${args.data}")
                println("callback = ${args.callback}")
            }
        }
        client.close()
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun closeEvent() {
        linkage.close()
    }
}
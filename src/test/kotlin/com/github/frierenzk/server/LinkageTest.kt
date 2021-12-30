package com.github.frierenzk.server

import com.github.frierenzk.config.ConfigEvent
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.TestUtils.waitingFor
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
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
            "set_add_task" to ConfigEvent.GetConfig,
            "set_stop_task" to PoolEvent.StopTask,
            "get_processing_list" to PoolEvent.ProcessingList,
            "get_task_status" to PoolEvent.GetTaskStatus,

            "reload_config" to ConfigEvent.Reload,
            "get_available_list" to ConfigEvent.GetConfigList,
            "get_task_config" to ConfigEvent.GetConfig,
            "set_add_config" to ConfigEvent.AddConfig,
            "set_change_config" to ConfigEvent.ModifyConfig,
            "set_delete_config" to ConfigEvent.DeleteConfig,

            "get_timer_config" to ConfigEvent.GetTicker,
            "add_ticker" to ConfigEvent.AddTicker,
            "modify_ticker" to ConfigEvent.ModifyTicker,
            "delete_ticker" to ConfigEvent.DeleteTicker,
        )
        testProject.forEach { (event, received) ->
            val ack = object : Ack {
                val channel by lazy { Channel<String>(1) }
                override fun call(vararg args: Any?) {
                    channel.trySendBlocking(args.joinToString())
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
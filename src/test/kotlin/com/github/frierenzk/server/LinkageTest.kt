package com.github.frierenzk.server

import com.github.frierenzk.MEvent
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

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
        linkage.sendEvent(ServerEvent.Default, 0)
    }

    @ExperimentalCoroutinesApi
    @Test
    @Order(3)
    fun listeners() {
        val client = IO.socket("http://127.0.0.1:21518")
        val connectChan = Channel<Unit>()
        client.on(Socket.EVENT_CONNECT) { println(Socket.EVENT_CONNECT).also { runBlocking { connectChan.send(Unit) } } }
        client.connect()
        val tmpTicker = ticker(3000)
        runBlocking {
            select<Unit> {
                tmpTicker.onReceive {
                    println("Connection time out")
                }
                connectChan.onReceive {
                    println("Remote server connected")
                }
            }
        }
        tmpTicker.cancel()
        connectChan.close()
        val testProject = hashMapOf(
            "set_add_task" to PoolEvent.AddTask,
            "set_stop_task" to PoolEvent.StopTask,
            "get_waiting_list" to PoolEvent.WaitingList,
            "get_processing_list" to PoolEvent.WorkingList,
            "get_available_list" to PoolEvent.AvailableList,
            "reload_config" to PoolEvent.ReloadConfig,
            "set_create_task" to PoolEvent.CreateTask
        )
        runBlocking { linkage.raisedEvent.receive() }
        testProject.forEach { (event, received) ->
            val ack = object : Ack {
                val channel by lazy { Channel<String>(1) }
                override fun call(vararg args: Any?) {
                    runBlocking { channel.send(args.joinToString()) }
                }
            }
            println("Now testing event $event")
            client.emit(event, "test", ack)
            val timer = ticker(3000)
            runBlocking {
                val data = select<String> {
                    ack.channel.onReceive { it }
                    timer.onReceive { "Out of time" }
                }
                println(data)
                val (rEvent, args) = select<Pair<EventType, Any>> {
                    linkage.raisedEvent.onReceive { it }
                    timer.onReceive { Pair(MEvent.Default, "") }
                }
                assertEquals(received, rEvent)
                println(args)
            }
            timer.cancel()
        }
        client.close()
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun closeEvent() {
        linkage.close()
    }
}
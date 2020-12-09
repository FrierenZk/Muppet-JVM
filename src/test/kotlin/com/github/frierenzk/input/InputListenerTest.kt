package com.github.frierenzk.input

import com.github.frierenzk.MEvent
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.TypeUtils.castMap
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import java.util.*

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class InputListenerTest {
    private val inputListener = InputListener().apply { init() }

    @Test
    @Order(1)
    fun getEventMonitor() {
        println(inputListener.eventMonitor)
        assertEquals(true, inputListener.eventMonitor.isNotEmpty())
    }

    @ExperimentalCoroutinesApi
    @Test
    @Order(2)
    fun receiveEvent() = runBlocking {
        inputListener.sendEvent(InputEvent.Default, 0)
        delay(1000)
        inputListener.sendEvent(MEvent.Exit, 0)
        delay(1000)
        assertEquals(true, inputListener.raisedEvent.isEmpty)
    }

    @ExperimentalCoroutinesApi
    @Test
    @Order(3)
    fun closeEvent() {
        inputListener.close()
        assertEquals(true, inputListener.raisedEvent.isClosedForReceive)
        assertEquals(false, inputListener.scope.isActive)
        assertEquals(false, inputListener.context.isActive)
    }

    @ExperimentalCoroutinesApi
    @Test
    @Order(4)
    fun inputTest() = runBlocking {
        val testFile =
            File("test.stream").apply {
                if (this.exists()) this.deleteRecursively()
                this.createNewFile()
                this.deleteOnExit()
            }
        val writer = testFile.bufferedWriter()
        writer.write("exit\n")
        writer.write("reload\n")
        writer.write("execute 1234\n")
        writer.write("stop 5678\n")
        writer.flush()
        writer.close()
        val listener = InputListener().apply {
            reader = Scanner(testFile)
            init()
        }
        delay(1000)
        assertEquals(false, listener.raisedEvent.isEmpty)
        assertEquals(MEvent.Exit, listener.raisedEvent.receive().first)
        assertEquals(PoolEvent.ReloadConfig, listener.raisedEvent.receive().first)
        listener.raisedEvent.receive().let {
            assertEquals(PoolEvent.AddTask, it.first)
            val pair = it.second as HashMap<*, *>
            assertEquals("1234", castMap<String, Any>(pair)["name"])
        }
        listener.raisedEvent.receive().let {
            assertEquals(PoolEvent.StopTask, it.first)
            val pair = it.second as Pair<*, *>
            assertEquals("5678", pair.second)
        }
        listener.reader.close()
        listener.close()
    }
}

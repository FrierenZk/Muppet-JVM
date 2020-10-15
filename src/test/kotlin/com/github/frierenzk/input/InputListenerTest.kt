package com.github.frierenzk.input

import com.github.frierenzk.MEvent
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.*

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class InputListenerTest {
    companion object {
        private val inputListener by lazy { InputListener() }
        private lateinit var out: ByteArrayOutputStream

        @BeforeAll
        @JvmStatic
        fun initAll() {
            out = ByteArrayOutputStream()
            System.setOut(PrintStream(out))
            System.setIn(InputStream.nullInputStream())
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            out.close()
            System.setOut(System.out)
            System.setIn(System.`in`)
        }
    }

    @BeforeEach
    fun init() {
        out.reset()
    }

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
        val reader = BufferedReader(StringReader(out.toString()))
        assertEquals(true, reader.ready())
        assertEquals("${InputEvent.Default} shouldn't be used", reader.readLine())
        out.reset()
        inputListener.sendEvent(MEvent.Exit, 0)
        delay(1000)
        assertEquals(0, out.toString().length)
    }

    @ExperimentalCoroutinesApi
    @Test
    @Order(3)
    fun inputTest() = runBlocking {
//        assertEquals(true, inputListener.raisedEvent.isEmpty)
//        System.setIn(ByteArrayInputStream("exit\n".toByteArray()))
//        assertEquals(com.github.frierenzk.MEvent.Exit, inputListener.raisedEvent.receive().first)
//        System.setIn(ByteArrayInputStream("reload\n".toByteArray()))
//        assertEquals(PoolEvent.ReloadConfig, inputListener.raisedEvent.receive().first)
//        System.setIn(ByteArrayInputStream("execute 1234\n".toByteArray()))
//        inputListener.raisedEvent.receive().let {
//            assertEquals(PoolEvent.AddTask, it.first)
//            assertEquals("1234", it.second as String)
//        }
//        System.setIn(ByteArrayInputStream("stop 5678\n".toByteArray()))
//        inputListener.raisedEvent.receive().let {
//            assertEquals(PoolEvent.StopTask, it.first)
//            assertEquals("5678", it.second as String)
//        }
    }

    @ExperimentalCoroutinesApi
    @Test
    @Order(4)
    fun closeEvent() {
        inputListener.close()
        assertEquals(true, inputListener.raisedEvent.isClosedForReceive)
        assertEquals(false, inputListener.scope.isActive)
        assertEquals(false, inputListener.context.isActive)
}    }

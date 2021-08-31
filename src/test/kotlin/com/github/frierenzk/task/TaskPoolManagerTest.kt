package com.github.frierenzk.task

import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.utils.MEvent
import com.github.frierenzk.utils.TestUtils.waitingFor
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TaskPoolManagerTest {
    companion object {
        private val pool by lazy { TaskPoolManager().apply { init() } }
        private val config by lazy { BuildConfig("111", "111", "111") }
    }

    @Test
    @Order(1)
    fun getEventMonitor() {
        println(pool.eventMonitor)
    }

    @Test
    @Order(2)
    fun receiveEvent() = runBlocking {
        pool.sendEvent(PoolEvent.Default, Pipe.default)
        pool.sendEvent(MEvent.Default, Pipe.default)
    }

    @Test
    @Order(3)
    fun getWorkingList() = runBlocking {
        val list = mutableListOf<String>()
        val channel = Channel<Unit>()
        pool.sendEvent(
            PoolEvent.WorkingList,
            Pipe.callback<List<String>> { list.addAll(it);channel.trySendBlocking(Unit) })
        waitingFor(channel, 1000)
        assertEquals(0, list.size)
        println(list)
    }

    @Test
    @Order(4)
    fun getWaitingList() = runBlocking {
        val list = mutableListOf<String>()
        val channel = Channel<Unit>()
        pool.sendEvent(
            PoolEvent.WaitingList,
            Pipe.callback<List<String>> { list.addAll(it);channel.trySendBlocking(Unit) })
        waitingFor(channel, 1000)
        assertEquals(0, list.size)
        println(list)
    }

    @Test
    @Order(5)
    fun setAddTask() = runBlocking {
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.CreateTask, Pipe<BuildConfig, String>(config) {
            string = it;channel.trySendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertTrue { string.isNotBlank() }
    }

    @Test
    @Order(6)
    fun getStatus() = runBlocking {
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.TaskStatus, Pipe<String, String>("111") {
            string = it;channel.trySendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals(TaskStatus.Null.toString(), string)
    }

    @Test
    @Order(7)
    fun setStopTask() = runBlocking {
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.StopTask, Pipe<String, String>("111") {
            string = it;channel.trySendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals("Can not find target task", string)
        pool.sendEvent(PoolEvent.CreateTask, Pipe<BuildConfig, String>(config) {})
        pool.sendEvent(PoolEvent.StopTask, Pipe<String, String>(config.name) {
            string = it;channel.trySendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertTrue { "Success" == string || "Can not find target task" == string }
    }

    @Test
    @Order(8)
    fun createNewTaskTest() = runBlocking {
        File("build/tmp/subversion").run {
            if (this.exists()) this.deleteRecursively()
        }
        val config = BuildConfig("testName", "test", "profile", hashMapOf(
            "svn" to "https://svn.apache.org/repos/asf/subversion/trunk/doc/programmer/",
            "source" to "build/tmp/subversion"
        ))
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.CreateTask, Pipe(config) { it: String ->
            string = it;channel.trySendBlocking(Unit)
        })
        waitingFor(channel, 10 * 1000)
        assertEquals("Success", string)
    }
}
package com.github.frierenzk.task

import com.github.frierenzk.MEvent
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.utils.TestUtils.waitingFor
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import kotlin.test.assertNotEquals

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TaskPoolManagerTest {
    companion object {
        private val pool by lazy { TaskPoolManager().apply { init() } }
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
    fun reloadConfig() = runBlocking {
        pool.sendEvent(PoolEvent.ReloadConfig, Pipe.callback<String> { })
        delay(10)
    }

    @Test
    @Order(4)
    fun getAvailableList() = runBlocking {
        val map = hashMapOf<String, String>()
        val channel = Channel<Unit>()
        pool.sendEvent(
            PoolEvent.AvailableList,
            Pipe.callback<Map<String, String>> { map.putAll(it);channel.sendBlocking(Unit) })
        waitingFor(channel, 2000)
        assertNotEquals(0, map.size)
        println(map)
    }

    @Test
    @Order(5)
    fun getWorkingList() = runBlocking {
        val list = mutableListOf<String>()
        val channel = Channel<Unit>()
        pool.sendEvent(
            PoolEvent.WorkingList,
            Pipe.callback<List<String>> { list.addAll(it);channel.sendBlocking(Unit) })
        waitingFor(channel, 1000)
        assertEquals(0, list.size)
        println(list)
    }

    @Test
    @Order(6)
    fun getWaitingList() = runBlocking {
        val list = mutableListOf<String>()
        val channel = Channel<Unit>()
        pool.sendEvent(
            PoolEvent.WaitingList,
            Pipe.callback<List<String>> { list.addAll(it);channel.sendBlocking(Unit) })
        waitingFor(channel, 1000)
        assertEquals(0, list.size)
        println(list)
    }

    @Test
    @Order(7)
    fun setAddTask() = runBlocking {
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.AddTask, Pipe<HashMap<String, Any>, String>(hashMapOf("name" to "1111")) {
            string = it;channel.sendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals("Can not find target task", string)
        pool.sendEvent(PoolEvent.AddTask, Pipe<HashMap<String, Any>, String>(hashMapOf("name" to "wifi6")) {
            string = it;channel.sendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals("Success", string)
    }

    @Test
    @Order(8)
    fun getStatus() = runBlocking {
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.TaskStatus, Pipe<String, String>("wifi6_new") {
            string = it;channel.sendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals(TaskStatus.Finished.toString(), string)
    }

    @Test
    @Order(9)
    fun setStopTask() = runBlocking {
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.StopTask, Pipe<String, String>("1111") {
            string = it;channel.sendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals("Can not find target task", string)
        pool.sendEvent(PoolEvent.AddTask, Pipe<String, String>("wifi6") {})
        pool.sendEvent(PoolEvent.StopTask, Pipe<String, String>("wifi6") {
            string = it;channel.sendBlocking(Unit)
        })
        waitingFor(channel, 1000)
        assertEquals("Success", string)
    }

    @Test
    @Order(10)
    fun createNewTaskTest() = runBlocking {
        File("build/tmp/subversion").run {
            if (this.exists()) this.deleteRecursively()
        }
        val map = hashMapOf(
            "name" to "testName",
            "category" to "test",
            "profile" to "profile",
            "svn" to "https://svn.apache.org/repos/asf/subversion/trunk/doc/programmer/",
            "sourcePath" to "build/tmp/subversion"
        )
        var string = ""
        val channel = Channel<Unit>()
        pool.sendEvent(PoolEvent.CreateTask, Pipe(map) { it: String ->
            string = it;channel.sendBlocking(Unit)
        })
        waitingFor(channel, 10 * 1000)
        assertEquals("Success", string)
    }
}
package com.github.frierenzk.task

import com.github.frierenzk.MEvent
import com.github.frierenzk.server.ServerEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TaskPoolManagerTest {
    private val pool by lazy { TaskPoolManager() }

    @Test
    @Order(1)
    fun getEventMonitor() {
        println(pool.eventMonitor)
    }

    @Test
    @Order(2)
    fun receiveEvent() = runBlocking {
        pool.sendEvent(PoolEvent.Default, 0)
        pool.sendEvent(MEvent.Default, 0)
    }

    @Test
    @Order(3)
    fun reloadConfig() = runBlocking {
        pool.sendEvent(PoolEvent.ReloadConfig, 0)
        delay(1000)
    }

    @Test
    @Order(4)
    fun getAvailableList() = runBlocking {
        pool.sendEvent(PoolEvent.AvailableList, 0)
        val (event, args) = pool.raisedEvent.receive()
        assertEquals(ServerEvent.AvailableList, event)
        println(args)
    }

    @Test
    @Order(5)
    fun getWorkingList() = runBlocking {
        pool.sendEvent(PoolEvent.WorkingList, 0)
        val (event, args) = pool.raisedEvent.receive()
        assertEquals(ServerEvent.WorkingList, event)
        println(args)
    }

    @Test
    @Order(6)
    fun getWaitingList() = runBlocking {
        pool.sendEvent(PoolEvent.WaitingList, 0)
        val (event, args) = pool.raisedEvent.receive()
        assertEquals(ServerEvent.WaitingList, event)
        println(args)
    }

    @Test
    @Order(7)
    fun setAddTask() = runBlocking {
        pool.sendEvent(PoolEvent.AddTask, "1111")
        pool.sendEvent(PoolEvent.AddTask, "wifi6_new")
        delay(1000)
    }

    @Test
    @Order(8)
    fun getStatus() = runBlocking {
        pool.sendEvent(PoolEvent.TaskStatus, "wifi6")
        val (event, args) = pool.raisedEvent.receive()
        assertEquals(ServerEvent.Status, event)
        println(args)
    }

    @Test
    @Order(9)
    fun setStopTask() = runBlocking {
        pool.sendEvent(PoolEvent.StopTask, "1111")
        pool.sendEvent(PoolEvent.AddTask, "wifi6")
        delay(50)
        pool.sendEvent(PoolEvent.StopTask, "wifi6")
        delay(1000)
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
        pool.sendEvent(PoolEvent.CreateTask, map)
        delay(10 * 1000)
    }
}
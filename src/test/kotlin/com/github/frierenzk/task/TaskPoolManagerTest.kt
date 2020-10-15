package com.github.frierenzk.task

import com.github.frierenzk.MEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import com.github.frierenzk.server.ServerEvent

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TaskPoolManagerTest {
    companion object{
        @BeforeAll
        @JvmStatic
        fun initAll(){}

        @AfterAll
        @JvmStatic
        fun tearDownAll(){}
    }

    private val pool by lazy { TaskPoolManager() }

    @BeforeEach
    fun init(){}

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
}
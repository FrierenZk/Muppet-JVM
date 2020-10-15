package com.github.frierenzk.server

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder

@ObsoleteCoroutinesApi
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class LinkageTest {
    companion object{
        @JvmStatic
        private val linkage = Linkage()
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

    @Test
    @Order(Int.MAX_VALUE)
    fun closeEvent() {
        linkage.close()
    }
}
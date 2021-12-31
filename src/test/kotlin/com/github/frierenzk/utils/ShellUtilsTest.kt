package com.github.frierenzk.utils

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ShellUtilsTest {
    @BeforeEach
    fun init() {
    }

    @Test
    @Order(1)
    fun exec() {
        val p = ShellUtils.exec(listOf("calc"))
        p.stop()
        println(p.returnCode)
    }

    @Test
    @Order(2)
    fun testIO() {
        val p = ShellUtils.exec(listOf("ping", "127.0.0.1", "-c", "4"))
        p.outBuffer?.lines()?.forEach { println(it) }
        p.errorBuffer?.lines()?.forEach { println(it) }
        println(p.returnCode)
    }

    @Test
    @Order(3)
    fun getReturnCode() {
        val p = ShellUtils.exec(listOf("aaaaa"))
        println(p.returnCode)
    }
}
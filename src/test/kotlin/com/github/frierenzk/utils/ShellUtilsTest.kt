package com.github.frierenzk.utils

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ShellUtilsTest {
    private lateinit var shell: ShellUtils

    @BeforeEach
    fun init() {
        shell = ShellUtils()
    }

    @Test
    @Order(1)
    fun exec() {
        shell.exec(listOf("calc"))
        shell.terminate()
        println(shell.returnCode)
    }

    @Test
    @Order(2)
    fun testIO() {
        shell.exec(listOf("ping", "127.0.0.1", "-c", "4"))
        shell.inputBuffer.lines().forEach { println(it) }
        shell.errorBuffer.lines().forEach { println(it) }
        println(shell.returnCode)
    }

    @Test
    @Order(3)
    fun getReturnCode() {
        shell.exec(listOf("aaaaa"))
        println(shell.returnCode)
    }
}
package com.github.frierenzk.task

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

internal class CreateNewCompileTaskTest {
    private val task by lazy { CreateNewCompileTask() }

    @ObsoleteCoroutinesApi
    @Test
    fun create() {
        try {
            File("build/tmp/subversion").deleteRecursively()
        } catch (exception: Exception) {
            println(exception)
        }
        var status = false
        task.onSave = {
            println(it)
            status = true
        }
        val map = hashMapOf<String, Any>(
            "name" to "testName",
            "category" to "test",
            "profile" to "profile",
            "svn" to "https://svn.apache.org/repos/asf/subversion/trunk/doc/programmer/",
            "sourcePath" to "build/tmp/subversion"
        )
        val (result, msg) = task.create(map)
        println(msg)
        assertEquals(result, true)
        assertEquals(status, true)

        task.onPush = { println(it) }
        task.run()
        runBlocking {
            while (!task.status.isEnd()) {
                delay(1000)
            }
        }
        task.close()
    }
}
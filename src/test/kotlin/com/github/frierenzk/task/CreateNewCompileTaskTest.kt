package com.github.frierenzk.task

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        val config = BuildConfig("testName",
            "test",
            "profile",
            hashMapOf("svn" to "https://svn.apache.org/repos/asf/subversion/trunk/doc/programmer/",
                "source" to "build/tmp/subversion"))
        task.create(config)
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
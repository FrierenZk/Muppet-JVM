package com.github.frierenzk.utils

import java.io.BufferedReader
import java.util.concurrent.TimeUnit

object ShellUtils {
    fun exec(command: List<String>): Process = object : Process() {
        override val process by lazy {
            try {
                ProcessBuilder("bash", "-c", command.joinToString(" ")).start()
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
        }

        override var outBuffer: BufferedReader? = process?.inputStream?.bufferedReader()
        override var errorBuffer: BufferedReader? = process?.errorStream?.bufferedReader()
    }

    @JvmName("execCommands")
    fun exec(commands: List<List<String>>): Process = object : Process() {
        override val process by lazy {
            try {
                ProcessBuilder("bash", "-c", commands.joinToString(" && ") { it.joinToString(" ") }).start()
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
        }

        override var outBuffer: BufferedReader? = process?.inputStream?.bufferedReader()
        override var errorBuffer: BufferedReader? = process?.errorStream?.bufferedReader()
    }

    abstract class Process {
        protected abstract val process: java.lang.Process?
        abstract var outBuffer: BufferedReader?
        abstract var errorBuffer: BufferedReader?
        val returnCode: Int
            get() = try {
                process?.waitFor(3, TimeUnit.SECONDS)
                process?.exitValue() ?: -1
            } catch (exception: IllegalThreadStateException) {
                exception.printStackTrace()
                -1
            }

        fun stop(): Int {
            closeHandle(process?.toHandle())
            return process?.exitValue() ?: -1
        }

        private fun closeHandle(p: ProcessHandle?) {
            p?.destroyForcibly()
            p?.descendants()?.forEach { closeHandle(it) }
        }

        val isAlive: Boolean
            get() = process?.isAlive ?: false
    }
}
package com.github.frierenzk.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString

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
        override var writer: BufferedWriter? = process?.outputWriter()
    }

    @JvmName("execCommands")
    fun exec(commands: List<List<String>>): Process = object : Process() {
        override val process by lazy {
            try {
                val file = createTempFile(suffix = ".sh",
                    attributes = arrayOf(PosixFilePermissions.asFileAttribute(setOf(
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_EXECUTE))))
                file.bufferedWriter().use { writer ->
                    commands.map { it.joinToString(" ") }.forEach {
                        writer.appendLine(it)
                    }
                    writer.flush()
                }
                ProcessBuilder("bash", "-c", "\"${file.pathString}\"").start().also {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(3 * 1000L)
                        runCatching { file.deleteIfExists() }.onFailure { it.printStackTrace() }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
        }

        override var outBuffer: BufferedReader? = process?.inputStream?.bufferedReader()
        override var errorBuffer: BufferedReader? = process?.errorStream?.bufferedReader()
        override var writer: BufferedWriter? = process?.outputWriter()
    }

    abstract class Process {
        protected abstract val process: java.lang.Process?
        abstract var outBuffer: BufferedReader?
        abstract var errorBuffer: BufferedReader?
        abstract var writer: BufferedWriter?
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
            if (process?.isAlive == true)
                process?.waitFor(3000, TimeUnit.MILLISECONDS)
            return try {
                process?.exitValue() ?: -1
            } catch (ignore: IllegalThreadStateException) {
                -1
            }
        }

        private fun closeHandle(p: ProcessHandle?) {
            p?.descendants()?.forEach { closeHandle(it) }
            if (p?.supportsNormalTermination() == true) p.destroy()
            else p?.destroyForcibly()
        }

        val isAlive: Boolean
            get() = process?.isAlive ?: false
    }
}
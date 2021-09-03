package com.github.frierenzk.task

import com.github.frierenzk.utils.ShellUtils
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.net.URI

sealed class SVNTask {
    companion object {
        fun buildSVNTask(uri: URI): SVNTask {
            if (!File(uri).isDirectory) throw IllegalArgumentException("Invalid target directory")
            return UpdateTask(uri)
        }

        fun buildSVNCheckOutTask(uri: URI, svn: String): SVNTask {
            if (!isURL(svn)) throw IllegalArgumentException("Invalid svn path")
            if (File(uri).exists() && (File(uri).listFiles()?.size
                    ?: 0) > 0
            ) throw IllegalArgumentException("Invalid checkout target directory")
            return CheckOutTask(uri, svn)
        }

        fun isURL(svn: String) = svn.startsWith("svn://") || svn.startsWith("http://") || svn.startsWith("https://")
    }

    protected open val uri by lazy { URI("").also { throw IllegalArgumentException("Not Implemented") } }
    protected open val svnPath by lazy { "".also { throw IllegalArgumentException("Not Implemented") } }

    protected val defaultBuffer by lazy { BufferedReader(StringReader("")) }

    var outBufferedReader = defaultBuffer
        protected set
    var errorBufferedReader = defaultBuffer
        protected set

    private fun notImplemented() {
        errorBufferedReader = BufferedReader(StringReader("this function current has not Implemented"))
    }

    open fun info(): String = notImplemented().let { "" }
    open fun update() = notImplemented()
    open fun checkOut() = notImplemented()

    private class UpdateTask(override val uri: URI) : SVNTask() {
        override fun info(): String {
            val info = ShellUtils.exec(listOf("svn", "info", uri.path))
            val list = info.outBuffer?.lineSequence()?.toList() ?: listOf()
            val rev = try {
                list.toList().takeIf { it.isNotEmpty() }
                    ?.first { it.startsWith("Last Changed Rev:") }?.trim() ?: ""
            } catch (exception: NoSuchElementException) {
                if (list.isEmpty()) ""
                else "unknown"
            }
            outBufferedReader = BufferedReader(StringReader(list.joinToString("\r\n")))
            errorBufferedReader = info.errorBuffer ?: defaultBuffer
            return rev
        }

        override fun update() {
            val update = ShellUtils.exec(listOf("svn", "update", uri.path))
            outBufferedReader = update.outBuffer ?: defaultBuffer
            errorBufferedReader = update.errorBuffer ?: defaultBuffer
        }
    }

    private class CheckOutTask(override val uri: URI, override val svnPath: String) : SVNTask() {
        override fun checkOut() {
            val checkOut = ShellUtils.exec(listOf("svn", "co", svnPath, uri.path))
            outBufferedReader = checkOut.outBuffer ?: defaultBuffer
            errorBufferedReader = checkOut.errorBuffer ?: defaultBuffer
        }

        override fun info(): String {
            val info = ShellUtils.exec(listOf("svn", "info", svnPath))
            val list = info.outBuffer?.lineSequence()?.toList() ?: listOf()
            val rev = try {
                list.takeIf { it.isNotEmpty() }?.first { it.contains("Last Changed Rev:") }
                    ?.substringAfter("Last Changed Rev:")?.trim() ?: ""
            } catch (exception: NoSuchElementException) {
                if (list.isEmpty()) ""
                else "unknown"
            }
            outBufferedReader = BufferedReader(StringReader(list.joinToString("\r\n")))
            errorBufferedReader = info.errorBuffer ?: BufferedReader(StringReader(""))
            return rev
        }
    }
}
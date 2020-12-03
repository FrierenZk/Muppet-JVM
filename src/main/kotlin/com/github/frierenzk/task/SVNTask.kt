package com.github.frierenzk.task

import com.github.frierenzk.utils.ShellUtils
import java.io.*
import java.net.URI

sealed class SVNTask {
    companion object {
        fun buildSVNTask(uri: URI): SVNTask {
            if (!File(uri).isDirectory) throw IllegalArgumentException("Invalid target directory")
            return UpdateTask(uri)
        }

        fun buildSVNCheckOutTask(uri: URI, svn: String): SVNTask {
            if (!svn.startsWith("svn://")) throw IllegalArgumentException("Invalid svn path")
            if (File(uri).listFiles()?.size ?: 0 > 0) throw IllegalArgumentException("Invalid checkout target directory")
            return CheckOutTask(uri, svn)
        }

        fun isSVNPath(svn: String) = svn.startsWith("svn://")
    }

    protected open val uri by lazy { URI("") }
    protected open val svnPath by lazy { "" }

    var outBufferedReader = BufferedReader(StringReader(""))
        protected set
    var errorBufferedReader = BufferedReader(StringReader(""))
        protected set

    protected fun notImplemented() {
        errorBufferedReader = BufferedReader(StringReader("this function current has not Implemented"))
    }

    open fun info() = notImplemented()
    open fun update() = notImplemented()
    open fun checkOut() = notImplemented()

    private class UpdateTask(override val uri: URI) : SVNTask() {
        override fun info() {
            val info = ShellUtils().apply { exec(listOf("svn", "info", uri.path)) }
            outBufferedReader = info.inputBuffer
            errorBufferedReader = info.errorBuffer
        }

        override fun update() {
            val update = ShellUtils().apply { exec(listOf("svn", "update", uri.path)) }
            outBufferedReader = update.inputBuffer
            errorBufferedReader = update.errorBuffer
        }
    }

    private class CheckOutTask(override val uri: URI, override val svnPath: String) : SVNTask() {
        override fun checkOut() {
            val checkOut = ShellUtils().apply { exec(listOf("svn", "co", svnPath, uri.path)) }
            outBufferedReader = checkOut.inputBuffer
            errorBufferedReader = checkOut.errorBuffer
        }
    }
}
package com.github.frierenzk.task

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.InvalidPathException

class CreateNewCompileTask : CompileTask() {
    private lateinit var task: SVNTask

    @ObsoleteCoroutinesApi
    override fun create(config: BuildConfig) {
        if (!SVNTask.isURL(config.getRemote()!!)) throw InvalidPathException(config.getRemote()!!, "Invalid svn url")
        task = SVNTask.buildSVNCheckOutTask(File(config.getSource()).toURI(), config.getRemote()!!)
        val rev = task.info()
        if (rev.isBlank()) throw InvalidPathException(config.getRemote()!!, "Can not get valid rev")
        task.outBufferedReader.forEachLine { onPush?.invoke(it) }
        task.errorBufferedReader.forEachLine { onPush?.invoke(it) }
        super.create(config)
    }

    override val runSequence = listOf(
        fun() = svnCheckOut(),
        fun() = imageClean(),
        fun() = imageBuild(),
        fun() = imageUpload()
    )

    private fun svnCheckOut(): TaskStatus {
        task.checkOut()
        scope.launch {
            task.errorBufferedReader.useLines { lines ->
                lines.forEach { onPush?.invoke(it) }
            }
        }
        task.outBufferedReader.useLines { lines ->
            lines.forEach { onPush?.invoke(it) }
        }
        return TaskStatus.Working
    }
}
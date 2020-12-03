package com.github.frierenzk.task

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File

class CreateNewCompileTask:CompileTask() {
    private lateinit var task: SVNTask
    var onSave: ((BuildConfig) -> Unit)? = null

    @ObsoleteCoroutinesApi
    fun create(name: String, category: String, profile: String, svn: String) =
        create(name, category, profile, svn, "", "", "")

    @ObsoleteCoroutinesApi
    fun create(
        name: String,
        category: String,
        profile: String,
        svn: String,
        projectDir: String,
        uploadPath: String,
        sourcePath: String
    ): Pair<Boolean, String> {
        if (!SVNTask.isURL(svn)) {
            return Pair(false, "Invalid svn url")
        }
        val config = BuildConfig().apply {
            this.name = name
            this.category = category
            this.profile = profile
            if (uploadPath.isNotBlank()) this.uploadPath = uploadPath
            if (projectDir.isNotBlank()) this.projectDir = projectDir
            if (sourcePath.isNotBlank()) this.sourcePath = sourcePath
        }
        try {
            task = SVNTask.buildSVNCheckOutTask(File(config.getFullSvnBasePath()).toURI(), svn)
        } catch (exception: IllegalArgumentException) {
            return Pair(false, exception.message ?: "")
        }
        val rev = task.info()
        task.outBufferedReader.forEachLine { onPush?.invoke(it) }
        task.errorBufferedReader.forEachLine { onPush?.invoke(it) }
        onSave?.invoke(config)
        super.create(config)
        return Pair(true, rev)
    }

    override val runSequence = listOf(
        fun() = svnCheckOut(),
        fun() = imageClean(),
        fun() = imageBuild(),
        fun() = imageUpload()
    )

    private fun svnCheckOut(): TaskStatus {
        task.checkOut()
        scope.launch(contextStdErr) {
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
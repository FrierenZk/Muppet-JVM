package com.github.frierenzk.task

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File

class CreateNewCompileTask : CompileTask() {
    private lateinit var task: SVNTask
    var onSave: ((BuildConfig) -> Unit)? = null

    @ObsoleteCoroutinesApi
    fun create(map: Map<String, Any>): Pair<Boolean, String> {
        val svn = map["svn"]?.let { it as? String } ?: ""
        if (!SVNTask.isURL(svn)) {
            return Pair(false, "Invalid svn url")
        }
        val config = BuildConfig()
        val paraMap = hashMapOf(
            "name" to fun(data: String) { config.name = data },
            "category" to fun(data: String) { config.category = data },
            "profile" to fun(data: String) { config.profile = data },
            "uploadPath" to fun(data: String) { config.uploadPath = data },
            "projectDir" to fun(data: String) { config.projectDir = data },
            "sourcePath" to fun(data: String) { config.sourcePath = data },
            "svnBasePath" to fun(data: String) { config.svnBasePath = data },
            "svn" to fun(_: String) {}
        )
        map.forEach { (key, value) ->
            if (paraMap.containsKey(key)) paraMap[key]?.invoke(value as? String ?: "")
            else config.extraParas[key] = value
        }
        try {
            task = SVNTask.buildSVNCheckOutTask(File(config.getFullSvnBasePath()).toURI(), svn)
        } catch (exception: IllegalArgumentException) {
            return Pair(false, exception.message ?: "")
        }
        val rev = task.info()
        if (rev.isBlank()) return Pair(false, "Can not get valid rev")
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
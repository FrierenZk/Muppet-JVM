package com.github.frierenzk.task

import com.github.frierenzk.utils.ShellUtils
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Path
import java.security.InvalidParameterException

open class CompileTask {
    companion object {
        @ObsoleteCoroutinesApi
        fun create(config: BuildConfig): CompileTask {
            return if (File(config.getSource()).let {
                    it.exists() && (it.listFiles()?.size ?: 0) > 0
                }) CompileTask().apply { create(config) }
            else if (config.getRemote() is String) CreateNewCompileTask().apply { create(config) }
            else throw InvalidParameterException(config.toString())
        }
    }

    private lateinit var config: BuildConfig
    val uid by lazy { config.projectDir ?: config.name }
    var status = TaskStatus.Waiting
    protected lateinit var scope: CoroutineScope
    private lateinit var context: ExecutorCoroutineDispatcher
    protected lateinit var contextStdErr: ExecutorCoroutineDispatcher
    var onPush: ((String) -> Unit)? = null
    var onUpdateStatus: (() -> Unit)? = null
    private lateinit var shell: ShellUtils

    @ObsoleteCoroutinesApi
    open fun create(config: BuildConfig) {
        this.config = config
        this.context = newSingleThreadContext(config.name)
        this.contextStdErr = newSingleThreadContext("${config.name}/stdErr")
        this.scope = CoroutineScope(context)
        status = TaskStatus.Waiting
    }

    protected open val runSequence = listOf(
        fun() = svnCheck(),
        fun() = imageClean(),
        fun() = imageBuild(),
        fun() = imageUpload()
    )

    fun run() {
        if (status.isWaiting()) status = TaskStatus.Working
        this.scope.launch(context) {
            try {
                runSequence.forEach {
                    if (!status.isEnd()) status = it()
                }
                if (!(status.isError() || status.isStopping())) {
                    status = TaskStatus.Finished.also {
                        onPush?.invoke("Finished")
                        onUpdateStatus?.invoke()
                    }
                } else if (status.isError()) {
                    onPush?.invoke("Error occurred")
                    onUpdateStatus?.invoke()
                }
            } catch (exception: Exception) {
                onPush?.invoke("Error occurred")
                exception.message?.let { onPush?.invoke(it) }
                exception.stackTrace.joinToString().let { onPush?.invoke(it) }
                status = TaskStatus.Error
            }
        }
    }

    private fun svnCheck(): TaskStatus {
        if (config.extraParas.containsKey("update") &&
            config.extraParas["update"] == false
        )
            return TaskStatus.Working
        onPush?.invoke("Check svn update")
        val task = SVNTask.buildSVNTask(File(config.getSource()).toURI())
        val rev = task.info()
        val printTask = fun(task: SVNTask) {
            task.outBufferedReader.forEachLine { onPush?.invoke(it) }
            task.errorBufferedReader.forEachLine { onPush?.invoke(it) }
        }
        printTask(task)
        if (rev.isBlank()) {
            onPush?.invoke("Check svn info error with rev: $rev")
            if (config.extraParas["buildOnlyIfUpdated"] == true) return TaskStatus.Error
        }
        task.update()
        printTask(task)
        if (config.extraParas.containsKey("buildOnlyIfUpdated") &&
            config.extraParas["buildOnlyIfUpdated"] == true
        ) {
            val updated = rev != task.info()
            printTask(task)
            println("updated = $updated rev = $rev")
            return if (updated) TaskStatus.Working
            else TaskStatus.Finished
        }
        return TaskStatus.Working
    }

    protected fun imageClean(): TaskStatus {
        onPush?.invoke("Clean images")
        val path = Path.of(config.getLocal() + "/Project/images")
        val file = path.toFile()
        if (!file.isDirectory) {
            onPush?.invoke("${file.absolutePath} is not a Directory")
            return TaskStatus.Error
        }
        file.listFiles()?.forEach {
            if (it.isFile) {
                it.delete()
                onPush?.invoke("delete ${it.name}")
            }
        }
        shell = ShellUtils().apply {
            execCommands(
                listOf(
                    "cd ${config.getLocal()}",
                    "./mkfw.sh ${config.profile} clean ; exit"
                )
            )
        }
        scope.launch(this.contextStdErr) {
            shell.errorBuffer?.useLines { lines ->
                lines.forEach { onPush?.invoke(it) }
            }
        }
        shell.inputBuffer?.useLines { lines ->
            lines.forEach { onPush?.invoke(it) }
        }
        return TaskStatus.Working
    }

    protected fun imageBuild(): TaskStatus {
        onPush?.invoke("Compile")
        @Suppress("SpellCheckingInspection")
        shell = ShellUtils().apply {
            execCommands(
                listOf(
                    "cd ${config.getLocal()}",
                    "./mkfw.sh ${config.profile} ; exit"
                )
            )
        }
        scope.launch(this.contextStdErr) {
            shell.errorBuffer?.useLines { lines ->
                lines.forEach { onPush?.invoke(it) }
            }
        }
        shell.inputBuffer?.useLines { lines ->
            lines.forEach { onPush?.invoke(it) }
        }
        onPush?.invoke("shell return code = ${shell.returnCode}")
        return if (shell.returnCode > 0) TaskStatus.Working
        else TaskStatus.Error
    }

    protected fun imageUpload(): TaskStatus {
        onPush?.invoke("Upload")
        val path = Path.of(config.getLocal() + "/Project/images")
        val file = path.toFile()
        val uploadFile = file.listFiles()?.find {
            it.extension.contains("gz")
        } ?: return TaskStatus.Error.also { onPush?.invoke("image is not exist") }
        if (uploadFile.length() < 1024 * 1024) {
            onPush?.invoke("image size is not right :${uploadFile.length() / 1024}KiB")
            return TaskStatus.Error
        }
        val command = listOf(
            "sudo", "sshpass", "-p", "654321", "scp",
            uploadFile.path, config.getUpload()
        )
        val upload = ShellUtils().apply { exec(command) }
        upload.inputBuffer?.forEachLine {
            onPush?.invoke(it)
        }
        upload.errorBuffer?.forEachLine {
            onPush?.invoke(it)
        }
        return if (upload.returnCode > 0) TaskStatus.Working
        else TaskStatus.Error
    }

    fun stop() {
        status = TaskStatus.Stopping
        onPush?.invoke("Stopping")
        onUpdateStatus?.invoke()
        if (this::shell.isInitialized && shell.alive) {
            shell.terminate()
        }
        status = TaskStatus.Finished
        onUpdateStatus?.invoke()
    }

    fun close() {
        scope.cancel("Close scope")
        context.close()
        contextStdErr.close()
        onPush?.invoke("Closed")
    }
}
package com.github.frierenzk.task

import kotlinx.coroutines.*
import com.github.frierenzk.utils.ShellUtils
import java.io.File
import java.nio.file.Path

open class CompileTask {
    private lateinit var config: BuildConfig
    val uid by lazy { config.projectDir ?: config.name }
    var status = TaskStatus.Waiting
    private lateinit var scope: CoroutineScope
    private lateinit var context: ExecutorCoroutineDispatcher
    private lateinit var contextStdErr: ExecutorCoroutineDispatcher
    var onPush: ((String) -> Unit)? = null
    var onUpdateStatus: (() -> Unit)? = null
    private lateinit var shell: ShellUtils

    @ObsoleteCoroutinesApi
    fun create(config: BuildConfig) {
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
        try {
            if (status == TaskStatus.Waiting) status = TaskStatus.Working
            this.scope.launch(context) {
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
            }
        } catch (exception: Exception) {
            onPush?.invoke("Error occurred")
            exception.message?.let { onPush?.invoke(it) }
            exception.stackTrace.joinToString().let { onPush?.invoke(it) }
            status = TaskStatus.Error
        }
    }

    private fun svnCheck(): TaskStatus {
        if (config.extraParas.containsKey("update") &&
            config.extraParas["update"] == false
        )
            return TaskStatus.Working
        onPush?.invoke("Check svn update")
        val task = SVNTask.buildSVNTask(File(config.getFullSvnBasePath()).toURI())
        val rev = task.info()
        val printTask = fun(task: SVNTask) {
            task.outBufferedReader.forEachLine { onPush?.invoke(it) }
            task.errorBufferedReader.forEachLine { onPush?.invoke(it) }
        }
        printTask(task)
        if (rev.isBlank()) {
            onPush?.invoke("Check svn info error with rev: $rev")
            return TaskStatus.Error
        }
        task.update()
        printTask(task)
        if (config.extraParas.containsKey("buildOnlyIfUpdated") &&
            config.extraParas["buildOnlyIfUpdated"] == true
        ) {
            val updated = rev == task.info()
            printTask(task)
            println("updated = $updated rev = $rev")
            return if (updated) TaskStatus.Working
            else TaskStatus.Finished
        }
        return TaskStatus.Working
    }

    protected fun imageClean(): TaskStatus {
        onPush?.invoke("Clean images")
        val path = Path.of(config.getFullSourcePath() + "/Project/images")
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
        return TaskStatus.Working
    }

    protected fun imageBuild(): TaskStatus {
        onPush?.invoke("Compile")
        @Suppress("SpellCheckingInspection")
        shell = ShellUtils().apply {
            execCommands(
                listOf(
                    "cd ${config.getFullSourcePath()}",
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
        val path = Path.of(config.getFullSourcePath() + "/Project/images")
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
            uploadFile.path, config.getFullUploadPath()
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
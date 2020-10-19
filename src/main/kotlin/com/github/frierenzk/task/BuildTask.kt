package com.github.frierenzk.task

import kotlinx.coroutines.*
import com.github.frierenzk.utils.ShellUtils
import java.nio.file.Path

class BuildTask {
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

    fun run() {
        try {
            if (status == TaskStatus.Waiting) status = TaskStatus.Working
            this.scope.launch(context) {
                if (status != TaskStatus.Error && status != TaskStatus.Stopping)
                    status = svnCheck()
                if (status != TaskStatus.Error && status != TaskStatus.Stopping)
                    status = imageClean()
                if (status != TaskStatus.Error && status != TaskStatus.Stopping)
                    status = imageBuild()
                if (status != TaskStatus.Error && status != TaskStatus.Stopping)
                    status = imageUpload()
                if (status != TaskStatus.Error && status != TaskStatus.Stopping) {
                    status = TaskStatus.Finished.also {
                        onPush?.invoke("Finished")
                        onUpdateStatus?.invoke()
                    }
                } else if (status == TaskStatus.Error) {
                    onPush?.invoke("Error occurred")
                    onUpdateStatus?.invoke()
                }
            }
        } catch (exception: Exception) {
            onPush?.invoke("Error occurred")
            exception.message?.let { onPush?.invoke(it) }
            status = TaskStatus.Error
        }
    }

    private fun svnCheck(): TaskStatus {
        if (config.extraParas.containsKey("update") &&
                config.extraParas["update"] == false)
            return TaskStatus.Working
        onPush?.invoke("Check svn update")
        val command = listOf("svn", "info", config.getFullSvnBasePath(), "|", "grep", "\"Last Changed Rev\"")
        val info = ShellUtils().apply { exec(command) }
        var rev = ""
        info.inputBuffer?.forEachLine {
            if (it.contains("Rev")) rev = it
            onPush?.invoke(it)
        }
        if (rev.isBlank()) {
            onPush?.invoke("Check svn info error with rev: $rev, return: ${info.returnCode}")
            return TaskStatus.Error
        }
        val update = ShellUtils().apply { exec(listOf("svn", "up", config.getFullSvnBasePath())) }
        update.inputBuffer?.forEachLine {
            onPush?.invoke(it)
        }
        if (config.extraParas.containsKey("buildOnlyIfUpdated") &&
                config.extraParas["buildOnlyIfUpdated"] == true) {
            info.exec(command)
            var updated = false
            info.inputBuffer?.forEachLine {
                if (it.contains("Rev")) updated = it != rev
                onPush?.invoke(it)
            }
            return if (!updated) TaskStatus.Finished
            else TaskStatus.Working
        }
        return TaskStatus.Working
    }

    private fun imageClean(): TaskStatus {
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

    @Suppress("SpellCheckingInspection")
    private fun imageBuild(): TaskStatus {
        onPush?.invoke("Compile")
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

    private fun imageUpload(): TaskStatus {
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
        val command = listOf("sudo", "sshpass", "-p", "654321", "scp",
                uploadFile.path, config.getFullUploadPath())
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
}
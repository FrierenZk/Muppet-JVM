package com.github.frierenzk.task

import com.github.frierenzk.config.IncompleteBuildConfig
import com.github.frierenzk.utils.ShellUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.channels.ClosedChannelException
import java.nio.file.Path
import java.security.InvalidParameterException
import java.util.*

@DelicateCoroutinesApi
class TaskEntity(val config: BuildConfig) {
    var status: TaskStatus = TaskStatus.Waiting
    var push: ((String) -> Unit)? = null
    var finish: ((TaskStatus) -> Unit)? = null
    var updateConfig: ((IncompleteBuildConfig) -> Unit)? = null
    val time = Calendar.getInstance().time

    private val scope by lazy { CoroutineScope(context) }
    private val context by lazy { newFixedThreadPoolContext(2, "${config.name}/main-${config["i"] ?: 0}") }
    private val stream by lazy { Channel<() -> Unit>(10) }
    private var currentShell: ShellUtils.Process? = null

    private val svn by lazy {
        object {
            fun info(url: String): List<String> {
                currentShell = ShellUtils.exec(listOf("svn", "info", url))
                return currentShell?.outBuffer?.lineSequence()?.toList() ?: listOf()
            }

            fun cleanup(uri: URI) {
                var error = false
                currentShell = ShellUtils.exec(listOf("svn", "cleanup", uri.path))
                scope.launch { currentShell?.outBuffer?.useLines { lines -> lines.forEach { push?.invoke(it) } } }
                currentShell?.errorBuffer?.useLines { lines ->
                    lines.forEach { push?.invoke(it);if (it.contains("svn: E")) error = true }
                }
                if (error) errorClean(InvalidParameterException("Error in svn operation"))
            }

            fun update(uri: URI) {
                var error = false
                if (config["buildOnlyIfUpdated"] == true || config["buildOnlyIfUpdated"] == "true") {
                    if (config.getRemote() !is String) errorClean(InvalidParameterException("Invalid svn url"))
                    val remote = rev(config.getRemote()!!)
                    if (remote.isBlank()) errorClean(InvalidParameterException("Can not read remote svn info"))
                    val local = rev(config.getSource())
                    if (remote == local) {
                        push?.invoke("No update with remote rev=$remote local rev=$local, task finished")
                        status = TaskStatus.Finished
                        stream.close()
                        return
                    }
                }
                val command = config["rev"]?.toString()?.toInt().let {
                    if (it is Int) listOf("svn", "update", uri.path, "-r", it.toString())
                    else listOf("svn", "update", uri.path)
                }
                currentShell = ShellUtils.exec(command)
                scope.launch {
                    currentShell?.outBuffer?.useLines { lines ->
                        lines.forEach {
                            push?.invoke(it)
                            if (it.contains("Authentication realm:")) currentShell?.writer?.write("654321\n")
                        }
                    }
                }
                currentShell?.errorBuffer?.useLines { lines ->
                    lines.forEach { push?.invoke(it);if (it.contains("svn: E")) error = true }
                }
                if (error) errorClean(InvalidParameterException("Error in svn operation"))
            }

            fun checkOut(url: String, target: URI) {
                var error = false
                val command = config["rev"]?.toString()?.toInt().let {
                    if (it is Int) listOf("svn", "checkout", url, target.path, "-r", it.toString())
                    else listOf("svn", "checkout", url, target.path)
                }
                currentShell = ShellUtils.exec(command)
                scope.launch {
                    currentShell?.outBuffer?.useLines { lines ->
                        lines.forEach {
                            push?.invoke(it)
                            if (it.contains("Authentication realm:")) currentShell?.writer?.write("654321\n")
                        }
                    }
                }
                currentShell?.errorBuffer?.useLines { lines ->
                    lines.forEach { push?.invoke(it);if (it.contains("svn: E")) error = true }
                }
                if (error) errorClean(InvalidParameterException("Error in svn operation"))
            }

            private fun rev(url: String): String {
                return info(url).findLast { it.startsWith("Revision: ") }?.replace("Revision: ", "") ?: ""
            }
        }
    }

    private val task by lazy {
        object {
            fun compile() {
                currentShell = ShellUtils.exec(listOf(listOf("cd", config.getLocal()),
                    listOf("./mkfw.sh", config.profile)))
                scope.launch { currentShell?.errorBuffer?.useLines { lines -> lines.forEach { push?.invoke(it) } } }
                currentShell?.outBuffer?.useLines { lines -> lines.forEach { push?.invoke(it) } }
                push?.invoke("Compile finished")
            }

            fun clean() {
                try {
                    val dir = Path.of(config.getLocal() + "/Project/images").toFile()
                    if (dir.exists() && dir.isDirectory)
                        dir.listFiles()?.filter { it.name.contains("tar.gz") }
                            ?.forEach { push?.invoke("Delete ${it.name}");it.delete() }
                } catch (exception: IOException) {
                    push?.invoke(exception.stackTraceToString())
                }
                currentShell = ShellUtils.exec(listOf(listOf("cd", config.getLocal()),
                    listOf("./mkfw.sh", config.profile, "clean")))
                scope.launch { currentShell?.errorBuffer?.useLines { lines -> lines.forEach { push?.invoke(it) } } }
                currentShell?.outBuffer?.useLines { lines -> lines.forEach { push?.invoke(it) } }
            }

            fun upload() {
                val dir = Path.of(config.getLocal() + "/Project/images").toFile()
                if (dir.exists() && dir.isDirectory) {
                    val file = dir.listFiles()?.findLast { it.name.contains("tar.gz") }
                    if (file is File) {
                        if (file.length() > 1024 * 1024) {
                            //Check path
                            val pathCheck: (String) -> Boolean = { path: String ->
                                var result = false
                                currentShell = ShellUtils.exec(listOf(
                                    listOf("if", "sshpass", "-p", BuildConfig.uploadPassword, "ssh",
                                        "${BuildConfig.uploadUser}@${BuildConfig.uploadAddress}",
                                        "'[ -d $path ]'"),
                                    listOf("then", "echo", "Yes"),
                                    listOf("else", "echo", "No"),
                                    listOf("fi")))
                                currentShell?.outBuffer?.useLines { lines ->
                                    lines.forEach { if (it.contains("Yes")) result = true }
                                }
                                result
                                //ShellUtils.exec(command).outBuffer?.readLines()?.any { it.contains("Yes") } ?: false
                            }
                            var path = "/volume1/version"
                            val pathArray = config.getUpload().let {
                                it.substring(it.indexOf(path) + path.length).split('/').filter { it.isNotBlank() }
                            }
                            for (seg in pathArray) {
                                path = "$path/$seg"
                                if (pathCheck(path)) continue
                                else currentShell =
                                    ShellUtils.exec(listOf("sshpass", "-p", BuildConfig.uploadPassword, "ssh",
                                        "${BuildConfig.uploadUser}@${BuildConfig.uploadAddress}", "mkdir", path))
                                        .also { push?.invoke("Create upload path $path") }
                                runBlocking { delay(100) }
                            }
                            //Upload
                            val command = listOf("sshpass", "-p", BuildConfig.uploadPassword,
                                "scp", file.path, config.getUpload())
                            var loop = true
                            while (loop) {
                                currentShell = ShellUtils.exec(command)
                                val list = currentShell?.outBuffer?.lineSequence()?.toList()?.filter { it.isNotBlank() }
                                    ?: listOf()
                                if (list.isEmpty()) {
                                    push?.invoke("Upload success")
                                    break
                                } else if (list.findLast { it.contains("lost connection") } is String) loop =
                                    status.isWorking()
                                else errorClean(Exception(list.joinToString("\r\n")))
                            }
                        } else errorClean(Exception("Image size is not right : ${file.length() / 1024}KiB"))
                    } else errorClean(Exception("Can not find image"))
                } else errorClean(Exception("Invalid directory path $dir"))
            }
        }
    }

    fun start() {
        scope.launch {
            try {
                stream.consumeEach { if (!status.isEnd()) it() }
                if (status.isWorking()) status = TaskStatus.Finished
            } catch (exception: Exception) {
                status = TaskStatus.Error
                push?.invoke(exception.stackTraceToString())
            } finally {
                finish?.invoke(status)
            }
        }
        scope.launch { initTask() }
    }

    fun stop() {
        status = TaskStatus.Stopping
        scope.launch { coroutineContext.cancelChildren() }
        stream.close()
        currentShell?.stop()
    }

    fun close() {
        scope.launch { coroutineContext.cancelChildren() }
        stream.close()
        currentShell?.stop()
        try {
            stream.tryReceive()
        } catch (ignore: Exception) {
        }
        scope.cancel()
        context.close()
    }

    private fun errorClean(cause: Exception) {
        status = TaskStatus.Error
        stream.close()
        try {
            stream.tryReceive()
        } catch (ignore: Exception) {
        }
        throw cause
    }

    private fun initTask() = scope.launch {
        status = TaskStatus.Working
        try {
            if (config.getRemote() !is String) stream.send { updateRemote() }
            if (svn.info(config.getSource()).findLast { it.startsWith("URL: ") } is String) stream.send {
                Path.of(config.getSource()).toUri().let {
                    svn.cleanup(it)
                    svn.update(it)
                }
            }
            else stream.send {
                Path.of(config.getSource()).toFile()
                    .let { if (!it.exists()) if (!it.mkdirs()) errorClean(InvalidParameterException("Can not create dir at ${it.path}")) }
                svn.checkOut(config.getRemote()!!, Path.of(config.getSource()).toUri())
            }
            stream.send { task.clean() }
            stream.send { task.compile() }
            stream.send { task.upload() }
            stream.send { stream.close() }
        } catch (ignore: ClosedChannelException) {
        }
    }

    private fun updateRemote() {
        val url = svn.info(config.getSource()).findLast { it.startsWith("URL: ") }?.replace("URL: ", "")
        if (url is String) {
            config["svn"] = url
            updateConfig?.invoke(IncompleteBuildConfig(name = config.name, extraParas = hashMapOf("svn" to url)))
        } else errorClean(InvalidParameterException("Can not read svn info in path \"${config.getSource()}\""))
    }
}
package com.github.frierenzk.task

import com.github.frierenzk.config.ConfigEvent
import com.github.frierenzk.config.IncompleteBuildConfig
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.server.ServerEvent
import com.google.gson.JsonObject
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ObsoleteCoroutinesApi
class TaskPoolManager : DispatcherBase() {
    override val eventMonitor = setOf(PoolEvent::class.java)

    private val checkContext by lazy { newSingleThreadContext("TaskPoolCheck") }
    private val checkTrigger by lazy { Channel<Unit>(4) }
    private val checkTicker by lazy { ticker(delayMillis = 30 * 1000L) }

    private val taskPool by lazy { ConcurrentHashMap<BuildConfig, TaskEntity>() }
    private val maxCount by lazy { Runtime.getRuntime().availableProcessors().let { if (it > 1) it else 1 } }
    private val calendarFormatter = SimpleDateFormat("[MM-dd HH:mm:ss]")

    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        when (event) {
            is PoolEvent ->
                when (event) {
                    PoolEvent.Default -> println("$event shouldn't be used")
                    PoolEvent.StopTask ->
                        args.runIf(args.isPipe<String, String>()) { stopTask(args.asPipe()!!) }
                    PoolEvent.WaitingList ->
                        args.runIf(args.isCallbackPipe<List<String>>()) { getWaitingList(args.asCallbackPipe()!!) }
                    PoolEvent.WorkingList ->
                        args.runIf(args.isCallbackPipe<List<String>>()) { getWorkingList(args.asCallbackPipe()!!) }
                    PoolEvent.TaskStatus ->
                        args.runIf(args.isPipe<String, String>()) { getTaskStatus(args.asPipe()!!) }
                    PoolEvent.CreateTask ->
                        args.runIf(args.isPipe<BuildConfig, String>()) { createTask(args.asPipe()!!) }
                }
        }
    }

    private fun printlnWithPushLogs(name: String, line: String) {
        if (line.isBlank()) return
        val time = calendarFormatter.format(Calendar.getInstance().time)
        runBlocking {
            raiseEvent(ServerEvent.BroadCast, Pipe.data(JsonObject().apply {
                addProperty("name", name)
                addProperty("time", time)
                addProperty("msg", line)
            }))
        }
        println("[$name]${time}: $line")
    }

    private fun taskCheck() {
        taskPool.filterValues { it.status.isEnd() }.forEach {
            it.value.close()
            taskPool.remove(it.key)
            printlnWithPushLogs(it.key.name, "Removed")
        }
        var count = taskPool.filter { it.value.status.isWorking() }.size
        for (i in taskPool.filterValues { it.status.isWaiting() }) {
            if (count >= maxCount) break
            if (taskPool.filterValues { it.status.isWorking() }.keys.map { it }
                    .findLast { it.conflicts(i.key) } != null) continue
            i.value.start()
            count++
        }
    }

    private fun createTask(args: Pipe<BuildConfig, String>) {
        if (taskPool.containsKey(args.data)) {
            args.callback("Target task duplicated")
            return
        }
        try {
            taskPool[args.data] = TaskEntity(args.data).apply {
                push = { printlnWithPushLogs(args.data.name, it) }
                finish = { runBlocking { checkTrigger.send(Unit) } }
                updateConfig = {
                    runBlocking {
                        raiseEvent(ConfigEvent.ModifyConfig,
                            Pipe<IncompleteBuildConfig, String>(it) {
                                printlnWithPushLogs(args.data.name, "Config updated with execution")
                            })
                    }
                }
            }
            checkTrigger.trySend(Unit)
            args.callback("Success")
        } catch (exception: Exception) {
            exception.printStackTrace()
            args.callback(exception.stackTraceToString())
        }
    }

    private fun stopTask(args: Pipe<String, String>) {
        val configs = taskPool.filterKeys { it.name == args.data }
        if (configs.isNotEmpty()) {
            configs.forEach { it.value.stop() }
            args.callback("Success")
        } else args.callback("Can not find target task")
    }

    private fun getWaitingList(args: Pipe<Unit, List<String>>) {
        val list = taskPool.filter { it.value.status.isWaiting() }.keys.map { it.name }
        args.callback(list)
    }

    private fun getWorkingList(args: Pipe<Unit, List<String>>) {
        val list = taskPool.filter { it.value.status.isWorking() }.keys.map { it.name }
        args.callback(list)
    }

    private fun getTaskStatus(args: Pipe<String, String>) {
        val tasks = taskPool.filterKeys { it.name == args.data }
        if (tasks.isEmpty()) {
            args.callback(TaskStatus.Null.toString())
            return
        }
        for (i in tasks) {
            if (i.value.status != TaskStatus.Waiting) {
                args.callback(i.value.status.toString())
                return
            }
        }
        args.callback(TaskStatus.Waiting.toString())
    }

    override fun init() {
        scope.launch(checkContext) {
            while (true) select<Unit> {
                checkTrigger.onReceive { taskCheck() }
                checkTicker.onReceive { taskCheck() }
            }
        }
    }

    override fun closeEvent() {
        checkContext.close()
        checkTicker.cancel()
        checkTrigger.close()
        super.closeEvent()
    }
}
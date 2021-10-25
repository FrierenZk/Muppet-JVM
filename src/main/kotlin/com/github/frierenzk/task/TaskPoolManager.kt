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

    private val taskPool by lazy { ConcurrentHashMap<Int, TaskEntity>() }
    private val maxCount by lazy { Runtime.getRuntime().availableProcessors().let { if (it > 1) it else 1 } }
    private val calendarFormatter = SimpleDateFormat("[MM-dd HH:mm:ss]")

    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        when (event) {
            is PoolEvent -> when (event) {
                PoolEvent.Default -> println("$event shouldn't be used")
                PoolEvent.CreateTask ->
                    args.runIf(args.isPipe<BuildConfig, String>()) { createTask(args.asPipe()!!) }
                PoolEvent.ProcessingList ->
                    args.runIf(args.isCallbackPipe<List<Int>>()) { getProcessList(args.asCallbackPipe()!!) }
                PoolEvent.StopTask ->
                    args.runIf(args.isPipe<Int, String>()) { stopTask(args.asPipe()!!) }
                PoolEvent.GetTaskStatus ->
                    args.runIf(args.isPipe<Int, String>()) { getTaskStatus(args.asPipe()!!) }
                PoolEvent.GetTaskName -> args.runIf(args.isPipe<Int, String>()) { getTaskName(args.asPipe()!!) }
            }
        }
    }

    private fun printlnWithPushLogs(name: Int, line: String) {
        if (line.isBlank()) return
        val time = calendarFormatter.format(Calendar.getInstance().time)
        runBlocking {
            raiseEvent(ServerEvent.BroadCast, Pipe.data(JsonObject().apply {
                addProperty("name", name.toString())
                addProperty("time", time)
                addProperty("msg", line)
            }))
        }
        println("[${taskPool[name]?.config?.name ?: "unknown"}]${time}: $line")
    }

    private fun taskCheck() {
        taskPool.filterValues { it.status.isEnd() }.forEach {
            it.value.close()
            taskPool.remove(it.key)
            printlnWithPushLogs(it.key, "Removed")
        }
        var count = taskPool.filter { it.value.status.isWorking() }.size
        for (i in taskPool.filterValues { it.status.isWaiting() }) {
            if (count >= maxCount) break
            if (taskPool.filterValues { it.status.isWorking() }.values.map { it }
                    .findLast { it.config.conflicts(i.value.config) } != null) continue
            i.value.start()
            count++
        }
    }

    private fun createTask(args: Pipe<BuildConfig, String>) {
        if (taskPool.containsKey(args.data.hashCode())) {
            args.callback("Target task duplicated")
            return
        }
        try {
            taskPool[args.data.hashCode()] = TaskEntity(args.data).apply {
                push = { printlnWithPushLogs(args.data.hashCode(), it) }
                finish = { runBlocking { checkTrigger.send(Unit) } }
                updateConfig = {
                    runBlocking {
                        raiseEvent(ConfigEvent.ModifyConfig,
                            Pipe<IncompleteBuildConfig, String>(it) {
                                printlnWithPushLogs(args.data.hashCode(), "Config updated with execution")
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

    private fun getProcessList(args: Pipe<Unit, List<Int>>) {
        val list = taskPool.keys.map { it.hashCode() }
        args.callback(list)
    }

    private fun stopTask(args: Pipe<Int, String>) {
        val task = taskPool[args.data]
        if (task is TaskEntity) {
            task.stop()
            args.callback("Success")
        } else args.callback("Can not find target task")
    }

    private fun getTaskStatus(args: Pipe<Int, String>) {
        val status = taskPool[args.data]?.status ?: TaskStatus.Null
        args.callback(status.toString())
    }

    private fun getTaskName(args: Pipe<Int, String>) {
        val name = taskPool[args.data]?.config?.name ?: "null"
        args.callback(name)
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
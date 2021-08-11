package com.github.frierenzk.task

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.server.ServerEvent
import com.github.frierenzk.utils.ConfigOperator
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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.concurrent.read
import kotlin.concurrent.write

@ObsoleteCoroutinesApi
class TaskPoolManager : DispatcherBase() {
    override val eventMonitor = setOf(PoolEvent::class.java)
    private val checkContext by lazy { newSingleThreadContext("TaskPoolCheck") }
    private val checkTrigger by lazy { Channel<String>(4) }
    private val checkTicker by lazy { ticker(delayMillis = 30 * 1000) }
    private val taskPool by lazy { ConcurrentHashMap<String, CompileTask>() }
    private val config by lazy { HashMap<String, BuildConfig>() }
    private val configLock by lazy { ReentrantReadWriteLock() }
    private var count = 0
    private val maxCount by lazy { Runtime.getRuntime().availableProcessors().let { if (it > 1) it else 1 } }
    private val calendarFormatter = SimpleDateFormat("[MM-dd HH:mm:ss]")

    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (event) {
            is PoolEvent ->
                when (event) {
                    PoolEvent.Default -> println("$event shouldn't be used")
                    PoolEvent.AddTask ->
                        args.runIf(args.isPipe<Map<String, Any>, String>()) { createNewTask(args.asPipe()!!) }
                    PoolEvent.StopTask ->
                        args.runIf(args.isPipe<String, String>()) { stopTask(args.asPipe()!!) }
                    PoolEvent.AvailableList ->
                        args.runIf(args.isCallbackPipe<Map<String, String>>()) { getAvailableList(args.asCallbackPipe()!!) }
                    PoolEvent.WaitingList ->
                        args.runIf(args.isCallbackPipe<List<String>>()) { getWaitingList(args.asCallbackPipe()!!) }
                    PoolEvent.WorkingList ->
                        args.runIf(args.isCallbackPipe<List<String>>()) { getWorkingList(args.asCallbackPipe()!!) }
                    PoolEvent.TaskStatus ->
                        args.runIf(args.isPipe<String, String>()) { getTaskStatus(args.asPipe()!!) }
                    PoolEvent.ReloadConfig ->
                        args.runIf(args.isCallbackPipe<String>()) { loadConfig(args.asCallbackPipe()!!) }
                    PoolEvent.CreateTask ->
                        args.runIf(args.isPipe<Map<String, Any>, String>()) { createNewCheckOutTask(args.asPipe()!!) }
                    else -> println(event)
                }
        }
    }

    private fun printlnWithPushLogs(name: String, line: String) {
        if (line.isBlank()) return
        val arr = "[$name]${calendarFormatter.format(Calendar.getInstance().time)}: $line"
        runBlocking { raiseEvent(ServerEvent.BroadCast, Pipe.data(Pair(name, arr + "\r\n"))) }
        println(arr)
    }

    private fun taskCheck() {
        val remove = HashSet<String>()
        val run = HashSet<String>()
        val working = HashSet<String>()
        taskPool.forEach {
            when (it.value.status) {
                TaskStatus.Finished -> remove.add(it.key)
                TaskStatus.Error -> remove.add(it.key)
                TaskStatus.Waiting -> if (count < maxCount) run.add(it.key)
                TaskStatus.Working -> working.add(it.value.uid)
                else -> Unit
            }
        }
        remove.forEach {
            println("removing $it")
            taskPool[it]?.close()
            taskPool.remove(it)
            count--
            runBlocking { raiseEvent(ServerEvent.TaskFinish, Pipe.data(it)) }
        }
        run.forEach {
            if (count < maxCount && !working.contains(taskPool[it]?.uid))
                taskPool[it]?.run()
        }
    }

    override fun closeEvent() {
        checkContext.close()
        checkTicker.cancel()
        checkTrigger.close()
        super.closeEvent()
    }

    private fun createNewTask(args: Pipe<out Map<String, Any>, String>) {
        val name = args.data["name"].let { it as? String } ?: ""
        val conf: BuildConfig? = configLock.read {
            if (config.containsKey(name))
                config[name]
            else null
        }
        if (conf is BuildConfig)
            if (taskPool.containsKey(name))
                args.callback("Target task duplicated")
            else {
                taskPool[name] = CompileTask().apply {
                    onPush = { printlnWithPushLogs(name, it) }
                    onUpdateStatus = { runBlocking { checkTrigger.send("") } }
                    create(conf.apply { extraParas.putAll(args.data) })
                }
                runBlocking { checkTrigger.send("") }
                args.callback("Success")
            }
        else args.callback("Can not find target task")
    }

    private fun createNewCheckOutTask(args: Pipe<out Map<String, Any>, String>) {
        try {
            val name = args.data["name"] as String
            if (config.containsKey(name)) {
                args.callback("Already had task, please change another name")
                return
            }
            val task = CreateNewCompileTask()
            var conf: BuildConfig? = null
            task.onSave = { conf = it }
            val (result, rev) = task.create(args.data)
            if (!result) {
                args.callback("Task Failed to create, with $rev")
                return
            }
            if (conf is BuildConfig) {
                configLock.read { config[name] = conf!! }
                args.callback("Success")
                ConfigOperator.saveBuildList(config)
                taskPool[name] = task.apply {
                    onPush = { printlnWithPushLogs(name, it) }
                    onUpdateStatus = { runBlocking { checkTrigger.send("") } }
                }
            } else args.callback("Can not generate BuildConfig from paras")
        } catch (exception: Exception) {
            args.callback(exception.stackTrace.joinToString("\r\n"))
            exception.printStackTrace()
        }
    }

    private fun stopTask(args: Pipe<String, String>) {
        if (!taskPool.containsKey(args.data)) {
            args.callback("Can not find target task")
            return
        }
        taskPool[args.data]?.stop()
        args.callback("Success")
    }

    private fun getAvailableList(args: Pipe<Unit, Map<String, String>>) {
        val list = hashMapOf<String, String>()
        config.forEach { (key, value) ->
            list[key] = value.category
        }
        args.callback(list)
    }

    private fun getWaitingList(args: Pipe<Unit, List<String>>) {
        val list = mutableListOf<String>()
        taskPool.forEach { (key, value) ->
            if (value.status.isWaiting()) list.add(key)
        }
        args.callback(list)
    }

    private fun getWorkingList(args: Pipe<Unit, List<String>>) {
        val list = mutableListOf<String>()
        taskPool.forEach { (key, value) ->
            if (value.status.isWorking())
                list.add(key)
        }
        args.callback(list)
    }

    private fun getTaskStatus(args: Pipe<String, String>) {
        val status = taskPool[args.data]?.status ?: TaskStatus.Finished
        args.callback(status.toString())
    }

    private fun loadConfig(args: Pipe<Unit, String>) = configLock.write {
        try {
            config.clear()
            config.putAll(ConfigOperator.loadBuildList())
            args.callback("Success")
            runBlocking { raiseEvent(ServerEvent.UpdateList, Pipe.default) }
            println(config.keys)
        } catch (exception: Exception) {
            args.callback("Load config failed with $exception")
            exception.printStackTrace()
        }
    }

    override fun init() {
        scope.launch(checkContext) {
            while (true)
                select<Unit> {
                    checkTrigger.onReceive { taskCheck() }
                    checkTicker.onReceive { taskCheck() }
                }
        }
        loadConfig(Pipe.callback { println("Config load $it") })
    }
}
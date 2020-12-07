package com.github.frierenzk.task

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.server.ServerEvent
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.io.File
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

    override fun receiveEvent(event: EventType, args: Any) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (event) {
            is PoolEvent ->
                when (event) {
                    PoolEvent.Default -> println("$event shouldn't be used")
                    PoolEvent.AddTask -> createNewTask(args)
                    PoolEvent.StopTask -> stopTask(args)
                    PoolEvent.AvailableList -> getAvailableList(args)
                    PoolEvent.WaitingList -> getWaitingList(args)
                    PoolEvent.WorkingList -> getWorkingList(args)
                    PoolEvent.TaskStatus -> getTaskStatus(args)
                    PoolEvent.ReloadConfig -> loadConfig()
                    PoolEvent.CreateTask -> createNewCheckOutTask(args)
                    else -> println(event)
                }
        }
    }

    private fun printlnWithPushLogs(name: String, line: String) = runBlocking {
        val arr = "[$name]${calendarFormatter.format(Calendar.getInstance().time)}: $line"
        println(arr).also { raiseEvent(ServerEvent.BroadCast, Pair(name, arr + "\r\n")) }
    }

    private fun taskCheck() = runBlocking {
        val remove = HashSet<String>()
        val run = HashSet<String>()
        val working = HashSet<String>()
        taskPool.forEach {
            raiseEvent(ServerEvent.Status, Pair(it.key, it.value.status))
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
        }
        run.forEach {
            if (count < maxCount && !working.contains(taskPool[it]?.uid))
                taskPool[it]?.run()
        }
    }

    private inline fun <reified T1, reified T2> castPairs(args: Pair<*, *>): Pair<T1?, T2?> {
        val (key, value) = args
        return if (key is T1 && value is T2) Pair(key, value)
        else Pair(null, null)
    }

    private inline fun <reified T1, reified T2> castMap(args: HashMap<*, *>): HashMap<T1, T2> {
        val dstMap = hashMapOf<T1, T2>()
        args.forEach { (key, value) ->
            if (key is T1 && value is T2) dstMap[key] = value
        }
        return dstMap
    }

    private fun createNewTask(arg: Any) {
        var uuid: UUID? = null
        var paras: HashMap<*, *>? = null
        val name = when (arg) {
            is String -> arg
            is Pair<*, *> -> {
                val (first, second) = arg
                if (first is UUID && second is String) {
                    uuid = first
                    second
                } else if (first is String && second is HashMap<*, *>) {
                    paras = second
                    first
                } else ""
            }
            else -> ""
        }
        val conf = configLock.read {
            if (config.containsKey(name))
                config[name]
            else null
        }
        if (conf is BuildConfig) {
            if (taskPool.containsKey(name)) {
                printlnWithPushLogs(name, "Target task duplicated")
            } else {
                if (paras is HashMap<*, *>) paras.forEach { (key, value) ->
                    if (key is String && value != null)
                        conf.extraParas[key] = value
                }
                taskPool[name] = CompileTask().apply {
                    create(conf)
                    onPush = fun(line: String) {
                        printlnWithPushLogs(name, line)
                    }
                    onUpdateStatus = fun() = runBlocking {
                        checkTrigger.send("")
                    }
                }
                runBlocking {
                    checkTrigger.send("")
                    if (uuid is UUID) raiseEvent(ServerEvent.AddTask, arg)
                    if (paras is HashMap<*, *>) printlnWithPushLogs(name, "timer Triggered")
                }
            }
        } else {
            printlnWithPushLogs(name, "Target task can not find in build list")
        }
    }

    private fun createNewCheckOutTask(args: Any) {
        var uuid: UUID? = null
        val push = fun(status: Boolean, msg: String) = runBlocking {
            val data = "[Create][${calendarFormatter.format(Calendar.getInstance().time)}]: $msg"
            println(data)
            raiseEvent(ServerEvent.CreateTask, Pair(uuid, mapOf("status" to status, "msg" to data)))
        }
        val map = castMap<String, Any>(args as? HashMap<*, *> ?: hashMapOf(0 to 0))
        try {
            val name = map["name"] as String
            val category = map["category"] as String
            val profile = map["profile"] as String
            val svn = map["svn"] as String
            val projectDir = map["projectDir"] as? String ?: ""
            val uploadPath = map["uploadPath"] as? String ?: ""
            val sourcePath = map["sourcePath"] as? String ?: ""
            uuid = map["UUID"] as? UUID
            if (config.containsKey(name)) {
                push.invoke(false, "Already had task $name, please change another name")
                return
            }
            val task = CreateNewCompileTask()
            var conf: BuildConfig? = null
            task.onSave = {
                conf = it
            }
            val (result, rev) = task.create(name, category, profile, svn, projectDir, uploadPath, sourcePath)
            if (!result) {
                push.invoke(false, "Task Failed to create, with $rev")
                return
            }
            if (conf is BuildConfig) {
                configLock.read {
                    config[name] = conf!!
                }
                push.invoke(true, "Task $name create success")
                saveConfig()
                taskPool[name] = task.apply {
                    onPush = fun(line: String) {
                        printlnWithPushLogs(name, line)
                    }
                    onUpdateStatus = fun() = runBlocking {
                        checkTrigger.send("")
                    }
                }
            } else {
                push.invoke(false, "Can not generate BuildConfig from paras")
                return
            }
        } catch (exception: Exception) {
            push.invoke(false, "Error occurred")
            push.invoke(false, exception.stackTraceToString())
            return
        }
    }

    private fun stopTask(args: Any) = runBlocking {
        val (uuid: UUID?, name: String?) = when (args) {
            is String -> Pair(null, args)
            is Pair<*, *> -> castPairs<UUID, String>(args)
            else -> Pair(null, null)
        }
        if (name is String)
            if (taskPool.containsKey(name)) {
                taskPool[name]?.stop()
                if (uuid is UUID) raiseEvent(ServerEvent.StopTask, args)
            } else {
                printlnWithPushLogs(name, "Target task can not find in build list")
            }
    }

    private fun getAvailableList(args: Any) = runBlocking {
        val list = hashMapOf<String, String>()
        config.forEach { (key, value) ->
            list[key] = value.category
        }
        raiseEvent(ServerEvent.AvailableList, Pair(args, list))
        checkTrigger.send("")
    }

    private fun getWaitingList(args: Any) = runBlocking {
        val list = mutableListOf<String>()
        taskPool.forEach { (key, value) ->
            if (value.status.isWaiting()) list.add(key)
        }
        raiseEvent(ServerEvent.WaitingList, Pair(args, list))
    }

    private fun getWorkingList(args: Any) = runBlocking {
        val list = mutableListOf<String>()
        taskPool.forEach { (key, value) ->
            if (value.status.isWorking())
                list.add(key)
        }
        raiseEvent(ServerEvent.WorkingList, Pair(args, list))
    }

    private fun getTaskStatus(args: Any) = runBlocking {
        val (arg, name) = when (args) {
            is Pair<*, *> -> args
            is String -> Pair(Unit, args)
            else -> throw IllegalArgumentException()
        }
        if (name is String) {
            val status = taskPool[name]?.status ?: TaskStatus.Finished
            raiseEvent(ServerEvent.Status, Pair(arg, Pair(name, status)))
        }
    }

    private fun loadConfig() {
        configLock.write {
            val gson = GsonBuilder().setPrettyPrinting().create() as Gson
            val file = File("build_list.json").apply { if (!this.exists()) this.createNewFile() }
            val reader = JsonReader(file.reader())
            val root = JsonParser.parseReader(reader) ?: null
            if (root is JsonElement && root.isJsonObject) {
                root.asJsonObject?.entrySet()?.forEach { (_, value) ->
                    if (value is JsonElement && value.isJsonObject) {
                        value.asJsonObject?.entrySet()?.forEach {
                            try {
                                val buildConfig = gson.fromJson(it?.value?.asJsonObject!!, BuildConfig::class.java)
                                    ?: null
                                if (buildConfig is BuildConfig)
                                    config[buildConfig.name] = buildConfig
                            } catch (exception: Exception) {
                                println(exception)
                            }
                        }
                    }
                }
            }
            println(config.keys)
        }
    }

    private fun saveConfig() {
        configLock.write {
            val file = File("build_list.json").apply {
                if (this.exists()) this.delete()
                this.createNewFile()
            }
            val map = HashMap<String, HashMap<String, BuildConfig>>()
            config.forEach { (key, value) ->
                map.getOrPut(value.category) { hashMapOf() }[key] = value
            }
            val json = GsonBuilder().setPrettyPrinting().create().toJson(map)
            map.forEach { (t, u) ->
                u.forEach { println("$t,$u,$it") }
            }
            file.bufferedWriter().run {
                write(json)
                flush()
                close()
            }
            println("Config Saved ${json.length} Bytes")
        }
    }

    init {
        scope.launch(checkContext) {
            while (true) {
                select<Unit> {
                    checkTrigger.onReceive {
                        taskCheck()
                    }
                    checkTicker.onReceive {
                        taskCheck()
                    }
                }
            }
        }
        loadConfig()
        BuildConfig.loadBuildConfigs()
    }
}
package com.github.frierenzk.ticker

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.ConfigOperator.loadTickerConfig
import com.github.frierenzk.utils.ConfigOperator.saveTickerConfig
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@ObsoleteCoroutinesApi
class TaskTicker : DispatcherBase() {
    override val eventMonitor by lazy { setOf(TickerEvent::class.java) }
    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        when (event) {
            is TickerEvent -> when (event) {
                TickerEvent.Default -> println("$event shouldn't be used")
                TickerEvent.Reset -> {
                    args.runIf(args.isCallbackPipe<String>()) { reset(args.asCallbackPipe()!!) }
                }
                TickerEvent.Enable ->
                    args.runIf(args.isPipe<String, String>()) { enableTask(args.asPipe()!!) }
                TickerEvent.Disable ->
                    args.runIf(args.isPipe<String, String>()) { disableTask(args.asPipe()!!) }
                TickerEvent.AddTimer ->
                    args.runIf(args.isPipe<Map<String, Any>, String>()) { addTimer(args.asPipe()!!) }
                TickerEvent.ModifyInterval ->
                    args.runIf(args.isPipe<Pair<String, Number>, String>()) { modifyInterval(args.asPipe()!!) }
            }
        }
    }

    internal val tasks: HashMap<String, Int> by lazy { hashMapOf() }
    private val taskParas: HashMap<String, HashMap<String, Any>> by lazy { hashMapOf() }
    private val lock by lazy { ReentrantReadWriteLock() }
    private val ticker by lazy { ticker(delayMillis = 60 * 1000) }
    private val tickerContext = newSingleThreadContext("ticker")
    private val stp = 1

    private fun reset(args: Pipe<Unit, String>) {
        try {
            val configs = loadTickerConfig().filter { it.value.getOrDefault("interval", Any()) is Number }
            configs.forEach { (t, u) -> u["name"] = t }
            println(configs.keys)
            lock.write {
                tasks.clear()
                taskParas.run { this.clear();this.putAll(configs) }
                configs.filterNot { it.value.getOrDefault("enable", true) == false }
                    .forEach { (key, _) -> tasks[key] = stp }
            }
            args.callback("Success")
        } catch (exception: Exception) {
            args.callback("Reset Failed with $exception")
            exception.printStackTrace()
        }
    }

    private fun enableTask(args: Pipe<String, String>) = lock.write {
        if (!taskParas.containsKey(args.data)) {
            args.callback("Can not find target task")
            return
        }
        if (tasks.containsKey(args.data)) {
            args.callback("Target task has enabled")
            return
        }
        taskParas[args.data]!!["enable"] = true
        tasks[args.data] = stp
        saveTickerConfig(taskParas)
        args.callback("Success")
    }

    private fun disableTask(args: Pipe<String, String>) = lock.write {
        if (!taskParas.containsKey(args.data)) {
            args.callback("Can not find target task")
            return
        }
        if (!tasks.containsKey(args.data)) {
            args.callback("Target task has disabled")
            return
        }
        taskParas[args.data]!!["enable"] = false
        tasks.remove(args.data)
        saveTickerConfig(taskParas)
        args.callback("Success")
    }

    private fun addTimer(args: Pipe<out Map<String, Any>, String>) {
        val name = args.data["name"].takeIf { it is String }.let { it as String }
        if (name.isBlank()) {
            args.callback("Invalid task name = $name")
            return
        }
        if (taskParas.containsKey(name)) {
            args.callback("Already have task name = $name")
            return
        }
        if (args.data["interval"] !is Number) {
            args.callback("Invalid task execute interval")
            return
        }
        val enabled = args.data["enable"]?.takeIf { it is Boolean }?.let { it as Boolean } ?: true
        lock.write {
            taskParas[name] = HashMap(args.data)
            if (enabled) tasks[name] = stp
            saveTickerConfig(taskParas)
            args.callback("Success")
        }
    }

    private fun modifyInterval(args: Pipe<out Pair<String, Number>, String>) {
        if (args.data.first.isBlank()) {
            args.callback("Invalid task name = ${args.data.first}")
            return
        }
        if (!taskParas.containsKey(args.data.first)) {
            args.callback("Can not find task name = ${args.data.first}")
            return
        }
        taskParas[args.data.first]!!["interval"] = args.data.second.toInt()
        saveTickerConfig(taskParas)
        args.callback("Success")
    }

    private fun tick() = lock.read {
        tasks.replaceAll { task, count ->
            val interval = (taskParas[task]?.get("interval") as Number).toInt()
            println(task)
            if (count > interval) {
                scope.launch(context) {
                    raiseEvent(
                        PoolEvent.AddTask,
                        Pipe<Map<String, Any>, String>(taskParas[task]?.filterNot { it.key == "interval" || it.key.isBlank() }
                            ?: mapOf()) {})
                }
                stp
            } else count + 1
        }
    }

    override fun closeEvent() {
        tickerContext.close()
        ticker.cancel()
        super.closeEvent()
    }

    override fun init() {
        reset(Pipe.callback { })
        scope.launch(tickerContext) {
            while (true) {
                ticker.receive()
                tick()
            }
        }
    }
}
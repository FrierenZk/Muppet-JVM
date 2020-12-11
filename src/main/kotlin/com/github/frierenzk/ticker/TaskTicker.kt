package com.github.frierenzk.ticker

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.ConfigOperator.loadTickerConfig
import com.github.frierenzk.utils.ConfigOperator.saveTickerConfig
import com.github.frierenzk.utils.TypeUtils.castMap
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
    override fun receiveEvent(event: EventType, args: Any) {
        when (event) {
            is TickerEvent -> when (event) {
                TickerEvent.Default -> println("$event shouldn't be used")
                TickerEvent.Enable -> enableTask(args)
                TickerEvent.Disable -> disableTask(args)
                TickerEvent.Reset -> reset()
                TickerEvent.AddTimer -> if (args is HashMap<*, *>) addTimer(args)
                TickerEvent.ModifyInterval -> if (args is HashMap<*, *>) modifyInterval(args)
            }
        }
    }

    internal val tasks: HashMap<String, Int> by lazy { hashMapOf() }
    private val taskParas: HashMap<String, HashMap<String, Any>> by lazy { hashMapOf() }
    private val lock by lazy { ReentrantReadWriteLock() }
    private val ticker by lazy { ticker(delayMillis = 60 * 1000) }
    private val tickerContext = newSingleThreadContext("ticker")
    private val stp = 1

    private fun reset() {
        val configs = loadTickerConfig().filter { it.value.getOrDefault("interval", Any()) is Number }
        configs.forEach { (t, u) -> u["name"] = t }
        println(configs.keys)
        lock.write {
            tasks.clear()
            taskParas.run { this.clear();this.putAll(configs) }
            configs.filterNot { it.value.getOrDefault("enable", true) == false }
                .forEach { (key, _) -> tasks[key] = stp }
        }
    }

    private fun enableTask(args: Any) = lock.write {
        val name = args as? String ?: ""
        if (!taskParas.containsKey(name)) return
        if (tasks.containsKey(name)) return
        taskParas[name]!!["enable"] = true
        tasks[name] = stp
        saveTickerConfig(taskParas)
    }

    private fun disableTask(args: Any) = lock.write {
        val name = args as? String ?: ""
        if (!taskParas.containsKey(name)) return
        if (!tasks.containsKey(name)) return
        taskParas[name]!!["enable"] = false
        tasks.remove(name)
        saveTickerConfig(taskParas)
    }

    private fun addTimer(args: HashMap<*, *>) {
        val config = castMap<String, Any>(args)
        val name = config["name"]?.takeIf { it is String }?.let { it as String } ?: ""
        if (name.isBlank()) return
        if (taskParas.containsKey(name)) return
        if (config["interval"] !is Number) return
        val enabled = config["enable"]?.takeIf { it is Boolean }?.let { it as Boolean } ?: true
        lock.write {
            taskParas[name] = config
            if (enabled) tasks[name] = stp
            saveTickerConfig(taskParas)
        }
    }

    private fun modifyInterval(args: HashMap<*, *>) {
        val config = castMap<String, Any>(args)
        val name = config["name"]?.takeIf { it is String }?.let { it as String } ?: ""
        val interval = config["interval"]?.takeIf { it is Number }?.let { it as Number }
        if (name.isBlank()) return
        if (!taskParas.containsKey(name)) return
        if (interval !is Number) return
        taskParas[name]!!["interval"] = interval.toInt()
        saveTickerConfig(taskParas)
    }

    private fun tick() = lock.read {
        tasks.replaceAll { task, count ->
            val interval = (taskParas[task]?.get("interval") as Number).toInt()
            println(task)
            if (count > interval) {
                scope.launch(context) {
                    raiseEvent(PoolEvent.AddTask, taskParas[task]?.filterNot {
                        it.key == "interval" || it.key.isBlank()
                    } ?: Unit)
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
        reset()
        scope.launch(tickerContext) {
            while (true) {
                ticker.receive()
                tick()
            }
        }
    }
}
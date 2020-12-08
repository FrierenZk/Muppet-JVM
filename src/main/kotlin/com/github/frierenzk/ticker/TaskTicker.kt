package com.github.frierenzk.ticker

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.utils.ConfigOperator
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@ObsoleteCoroutinesApi
class TaskTicker: DispatcherBase() {
    override val eventMonitor by lazy { setOf(TickerEvent::class.java) }
    override fun receiveEvent(event: EventType, args: Any) {
        when (event) {
            is TickerEvent -> when (event) {
                TickerEvent.Default -> println("$event shouldn't be used")
                TickerEvent.Reset -> reset()
            }
        }
    }

    private val tasks: HashMap<String, Int> by lazy { hashMapOf() }
    private val taskParas: HashMap<String, HashMap<String, Any>> by lazy { hashMapOf() }
    private val lock by lazy { ReentrantReadWriteLock() }
    private val ticker by lazy { ticker(delayMillis = 60 * 1000) }
    private val tickerContext = newSingleThreadContext("ticker")

    private fun reset() {
        val configs = ConfigOperator.loadTickerConfig()
        println(configs)
        lock.write {
            tasks.clear()
            taskParas.clear()
            taskParas.putAll(configs)
            configs.forEach { (key, _) -> tasks[key] = 0 }
        }
    }

    private fun tick() {
        lock.read {
            tasks.replaceAll { task, count ->
                val interval = taskParas[task]?.get("interval").toString().toDoubleOrNull() ?: Double.POSITIVE_INFINITY
                if (count > interval) {
                    scope.launch(context) {
                        raiseEvent(PoolEvent.AddTask, Pair(task, taskParas[task]?.filterNot {
                            it.key == "interval" || it.key == "" || it.key == "name"
                        }))
                    }
                    1
                } else count + 1
            }
        }
    }

    init {
        reset()
        scope.launch(tickerContext) {
            while (true) {
                ticker.receive()
                tick()
            }
        }
    }
}
package com.github.frierenzk.ticker

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.io.File
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
        lock.write {
            val file = File("timer.json")
            if (!(file.exists() && file.isFile)) return
            tasks.clear()
            taskParas.clear()
            val gson = GsonBuilder().setPrettyPrinting().create()!!
            val root = JsonParser.parseReader(JsonReader(file.reader())) ?: null
            if (root is JsonElement && root.isJsonArray) {
                root.asJsonArray?.forEach {
                    try {
                        val map = gson.fromJson<HashMap<String, Any>>(it, HashMap::class.java) ?: null
                        if (map is HashMap<String, Any>) {
                            val name = map["name"]
                            val interval = (map["interval"] ?: -1).toString().toDoubleOrNull()
                            if (name is String && interval is Double && interval > 0) {
                                map["interval"] = interval
                                tasks[name] = 0
                                taskParas[name] = map.apply { this["buildOnlyIfUpdated"] = true }
                            } else println(interval)
                        }
                    } catch (exception: Exception) {
                        println(exception)
                    }
                }
            }
            println(tasks)
        }
    }

    init {
        runBlocking {
            reset()
        }
        scope.launch(tickerContext) {
            while (true) {
                ticker.receive()
                lock.read {
                    tasks.replaceAll { task, count ->
                        val interval = taskParas[task]?.get("interval") as Double
                        if (count >= interval) {
                            runBlocking {
                                raiseEvent(PoolEvent.AddTask, Pair(task, taskParas[task]?.filterNot {
                                    it.key == "interval" || it.key == "" || it.key == "name"
                                }))
                            }
                            0
                        }
                        else count+1
                    }
                }
            }
        }

    }
}
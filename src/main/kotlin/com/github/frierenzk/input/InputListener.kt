package com.github.frierenzk.input

import com.github.frierenzk.config.ConfigEvent
import com.github.frierenzk.config.IncompleteBuildConfig
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.ticker.TickerEvent
import com.github.frierenzk.utils.MEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.util.*

@ObsoleteCoroutinesApi
class InputListener : DispatcherBase() {
    private val inputContext by lazy { newSingleThreadContext("input") }
    private val handlerContext by lazy { newSingleThreadContext("handler") }
    override val eventMonitor by lazy { setOf(InputEvent::class.java) }
    internal var reader = Scanner(System.`in`)

    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        when (event) {
            is InputEvent -> when (event) {
                InputEvent.Default -> println("$event shouldn't be used")
            }
        }
    }

    override fun closeEvent() {
        inputContext.close()
        handlerContext.close()
        reader.close()
        super.closeEvent()
    }

    private suspend fun handleInput(line: String) {
        val list = line.split(Regex("[ \n\t\r\"]")).filterNot {
            it.isBlank()
        }
        when (list.getOrNull(0)) {
            "exit" -> raiseEvent(MEvent.Exit, Pipe.default)
            "reload" -> raiseEvent(ConfigEvent.Reload, Pipe.callback<String> { println(it) })
            "reload-force" -> raiseEvent(ConfigEvent.Reload, Pipe<Boolean, String>(true) { println(it) })
            "save" -> raiseEvent(ConfigEvent.Save, Pipe.callback<String> { println(it) })
            "execute" -> list.getOrNull(1).let { args ->
                if (args is String) raiseEvent(
                    ConfigEvent.GetConfig,
                    Pipe<String, BuildConfig?>(args) {
                        if (it is BuildConfig) list.getOrNull(2).let { force ->
                            runBlocking {
                                raiseEvent(
                                    PoolEvent.CreateTask,
                                    Pipe<BuildConfig, String>(
                                        if (force == "-f") it + IncompleteBuildConfig(
                                            extraParas = hashMapOf("i" to Calendar.getInstance().timeInMillis)
                                        )
                                        else it) { ret -> println("[$args]$ret") })
                            }
                        }
                        else println("[Input]Can not find \"$args\"")
                    }
                )
            }
            "stop" -> list.getOrNull(1).let { args ->
                if (args is String) {
                    raiseEvent(PoolEvent.ProcessingList, Pipe.callback<List<Int>> {
                        it.forEach { task ->
                            runBlocking {
                                raiseEvent(PoolEvent.GetTaskName, Pipe<Int, String>(task) {
                                    if (it == args) runBlocking {
                                        raiseEvent(PoolEvent.StopTask,
                                            Pipe<Int, String>(task) { println("[Input]: stop $args info = $it") })
                                    }
                                })
                            }
                        }
                    })
                }
            }
            "trigger" -> list.getOrNull(1).let { args ->
                if (args is String) raiseEvent(TickerEvent.Trigger,
                    Pipe<String, String>(args) { println("[Input] trigger $args info = $it") })
            }
            "working" -> raiseEvent(PoolEvent.ProcessingList, Pipe.callback<List<Int>> {
                if (it.isNotEmpty()) it.forEach {
                    val channel = arrayOf(Channel<String>(1), Channel(1))
                    runBlocking {
                        raiseEvent(PoolEvent.GetTaskName,
                            Pipe<Int, String>(it) { channel[0].trySendBlocking(it) })
                        raiseEvent(PoolEvent.GetTaskStatus,
                            Pipe<Int, String>(it) { channel[1].trySendBlocking(it) })
                    }
                    scope.launch { println("${channel[0].receive()}:${channel[1].receive()}") }
                }
                else println("No working tasks")
            })
        }
    }

    override fun init() {
        scope.launch(inputContext) {
            while (status) try {
                if (reader.hasNextLine()) {
                    val line = reader.nextLine() ?: null
                    if (line is String) {
                        launch(handlerContext) { handleInput(line) }
                    }
                }
            } catch (ignored: IllegalStateException) {
            }
        }
    }
}
package com.github.frierenzk.input

import com.github.frierenzk.MEvent
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.ticker.TickerEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.*

@ObsoleteCoroutinesApi
class InputListener:DispatcherBase() {
    private val inputContext = newSingleThreadContext("com/github/frierenzk/input")
    private val handlerContext by lazy { newSingleThreadContext("handler") }
    override val eventMonitor by lazy { setOf(InputEvent::class.java) }
    internal var reader = Scanner(System.`in`)

    override fun receiveEvent(event: EventType, args: Any) {
        when (event) {
            is InputEvent -> when (event) {
                InputEvent.Default -> println("$event shouldn't be used")
            }
        }
    }

    override fun closeEvent() {
        inputContext.close()
        super.closeEvent()
    }

    private suspend fun handleInput(line: String) {
        val list = line.split(Regex("[ \n\t\r\"]")).filterNot {
            it.isBlank()
        }
        when (list.getOrNull(0)) {
            "exit" -> raiseEvent(MEvent.Exit, 0.0f)
            "reload" -> raiseEvent(PoolEvent.ReloadConfig, 0)
            "execute" -> {
                val args = list.getOrNull(1)
                if (args is String) raiseEvent(PoolEvent.AddTask, Pair(null, hashMapOf("name" to args)))
            }
            "stop" -> {
                val args = list.getOrNull(1)
                if (args is String) raiseEvent(PoolEvent.StopTask, Pair(null, args))
            }
            "resetTicker" -> raiseEvent(TickerEvent.Reset, 0)
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
package com.github.frierenzk.ticker

import com.github.frierenzk.config.ConfigEvent
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.task.PoolEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

@ObsoleteCoroutinesApi
class TaskTicker : DispatcherBase() {
    override val eventMonitor by lazy { setOf(TickerEvent::class.java) }
    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        when (event) {
            is TickerEvent -> when (event) {
                TickerEvent.Default -> println("$event shouldn't be used")
                TickerEvent.Update -> args.runIf(args.isPipe<Map<String, TickerConfig>, String>()) { update(args.asPipe()!!) }
                TickerEvent.Trigger -> args.runIf(args.isPipe<String, String>()) { trigger(args.asPipe()!!) }
            }
        }
    }

    private val ticker by lazy { ticker(delayMillis = 60 * 1000L) }
    private val tickerContext = newSingleThreadContext("ticker")
    private val triggers by lazy { ConcurrentHashMap<String, Trigger>() }

    private fun update(args: Pipe<Map<String, TickerConfig>, String>) {
        triggers.clear()

        triggers.putAll(args.data.map { Pair(it.key, Trigger(it.value)) })
        triggers.forEach {
            it.value.launch = { launch(it.key, it.value) }
        }
        args.callback("Success")
    }

    private fun trigger(args: Pipe<String, String>) {
        triggers[args.data].let {
            if (it is Trigger) {
                it.knock(true)
                args.callback("Success")
            } else args.callback("Can not find ${args.data}")
        }
    }

    private fun launch(name: String, trigger: Trigger) = runBlocking {
        val pipe = { config: BuildConfig -> Pipe<BuildConfig, String>(config) { println("[TaskTicker]: Start $name") } }
        trigger.tickerConfig.let { config ->
            when (config) {
                is TickerConfig.Config -> raiseEvent(PoolEvent.CreateTask, pipe(config.config))
                is TickerConfig.Ref -> raiseEvent(ConfigEvent.GetConfig, Pipe<String, BuildConfig?>(config.ref) {
                    if (it is BuildConfig) runBlocking { raiseEvent(PoolEvent.CreateTask, pipe(it)) }
                    else println("[TaskTicker]: Can not find ref ${config.ref}")
                })
                is TickerConfig.Mix -> raiseEvent(ConfigEvent.GetConfig, Pipe<String, BuildConfig?>(config.ref) {
                    if (it is BuildConfig) runBlocking { raiseEvent(PoolEvent.CreateTask, pipe(it + config.config)) }
                    else println("[TaskTicker]: Can not find ref ${config.ref}")
                })
            }
        }
    }

    private fun tick() = triggers.forEach { it.value.knock() }

    override fun closeEvent() {
        tickerContext.close()
        ticker.cancel()
        super.closeEvent()
    }

    override fun init() {
        scope.launch(tickerContext) {
            while (true) {
                ticker.receive()
                tick()
            }
        }
    }
}
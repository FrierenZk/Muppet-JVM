package com.github.frierenzk.dispatcher

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

@ObsoleteCoroutinesApi
abstract class DispatcherBase : IDispatcher {
    final override val context by lazy {
        newSingleThreadContext(this::class.simpleName!!).also {
            println(this::class.simpleName)
        }
    }
    val scope by lazy { CoroutineScope(context) }
    protected var status = true
    private val channel by lazy { Channel<Pair<EventType, Pipe<*, *>>>(10) }
    final override val raisedEvent by lazy { Channel<Pair<EventType, Pipe<*, *>>>(10) }
    override val eventMonitor: Set<Class<out Any>> by lazy { setOf() }

    final override suspend fun sendEvent(event: EventType, args: Pipe<*, *>) {
        channel.send(Pair(event, args))
    }

    protected suspend fun raiseEvent(event: EventType, args: Pipe<*, *>) {
        raisedEvent.send(Pair(event, args))
    }

    protected open fun receiveEvent(event: EventType, args: Pipe<*, *>) {}

    final override fun close() {
        status = false
        runBlocking { launch { closeEvent() } }
        channel.close()
        raisedEvent.close()
        scope.cancel()
        context.close()
    }

    protected open fun closeEvent() {}

    init {
        scope.launch(context) {
            for (value in channel) {
                receiveEvent(value.first, value.second)
            }
        }
    }
}
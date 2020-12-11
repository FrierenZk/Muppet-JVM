package com.github.frierenzk.dispatcher

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.channels.Channel

interface DispatcherInterface {
    val raisedEvent: Channel<Pair<EventType, Any>>
    val context: ExecutorCoroutineDispatcher
    val eventMonitor: Set<Class<out Any>>
    suspend fun sendEvent(event: EventType, args: Any)
    fun init()
    fun close()
}
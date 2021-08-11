package com.github.frierenzk.dispatcher

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.channels.Channel

interface IDispatcher {
    val raisedEvent: Channel<Pair<EventType, Pipe<*, *>>>
    val context: ExecutorCoroutineDispatcher
    val eventMonitor: Set<Class<out Any>>
    suspend fun sendEvent(event: EventType, args: Pipe<*, *>)
    fun init()
    fun close()
}
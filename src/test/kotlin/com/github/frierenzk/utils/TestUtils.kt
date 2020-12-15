package com.github.frierenzk.utils

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select

object TestUtils {
    @ObsoleteCoroutinesApi
    suspend fun waitingFor(channel: Channel<out Any>, outTime: Long): Any {
        val ticker = ticker(outTime)
        return select<Any> {
            channel.onReceive { it }
            ticker.onReceive { "Time out" }
        }.also { ticker.cancel() }
    }
}
package com.github.frierenzk

import com.github.frierenzk.config.ConfigCenter
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.IDispatcher
import com.github.frierenzk.input.InputListener
import com.github.frierenzk.server.Linkage
import com.github.frierenzk.task.TaskPoolManager
import com.github.frierenzk.ticker.TaskTicker
import com.github.frierenzk.utils.MEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.system.exitProcess

private val channel by lazy { Channel<Int>() }
private val handlerCollections by lazy { HashSet<IDispatcher>() }

@ObsoleteCoroutinesApi
private suspend fun preInit() = coroutineScope {
    launch { handlerCollections.add(ConfigCenter().apply { init() }) }
    launch { handlerCollections.add(TaskPoolManager().apply { init() }) }
    launch { handlerCollections.add(Linkage().apply { init() }) }
    launch { handlerCollections.add(InputListener().apply { init() }) }
    launch { handlerCollections.add(TaskTicker().apply { init() }) }
}

private suspend fun handleEvent(event: EventType, args: Any) {
    when (event) {
        MEvent.Exit -> {
            channel.send(args.toString().toIntOrNull() ?: 0)
            channel.close()
        }
    }
}

private suspend fun runDispatcher() = coroutineScope {
    val returnCode = async { channel.receive() }
    val jobs = mutableListOf<Job>()
    handlerCollections.forEach { handler ->
        jobs += launch(handler.context) {
            for ((event, args) in handler.raisedEvent)
                if (event is MEvent)
                    launch { handleEvent(event, args) }
                else handlerCollections.forEach {
                    if (it.eventMonitor.contains(event::class.java))
                        launch { it.sendEvent(event, args) }
                    //else println("${it.eventMonitor}, ${value.first::class.java}")
                }
        }
    }
    println("Process finished with exit code ${returnCode.await()}")
    jobs.forEach {
        launch { it.cancelAndJoin() }
    }
}

private fun postClean() {
    handlerCollections.parallelStream().forEach {
        it.close()
    }
}

@ObsoleteCoroutinesApi
suspend fun main(): Unit = coroutineScope {
    try {
        preInit()
        runDispatcher()
        postClean()
        exitProcess(0)
    } catch (exception: Exception) {
        println(exception)
        exitProcess(-1)
    }
}
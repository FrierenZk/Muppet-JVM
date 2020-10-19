package com.github.frierenzk

import com.github.frierenzk.dispatcher.DispatcherInterface
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.input.InputListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import com.github.frierenzk.server.Linkage
import com.github.frierenzk.task.TaskPoolManager
import com.github.frierenzk.ticker.TaskTicker
import kotlin.system.exitProcess

@ObsoleteCoroutinesApi
class Muppet {
    companion object {
        private val channel by lazy { Channel<Int>() }
        private val handlerCollections by lazy { HashSet<DispatcherInterface>() }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                preInit()
                runDispatcher()
                postClean()
            } catch (exception: Exception) {
                println(exception)
                exitProcess(-1)
            }
        }

        private fun preInit() = runBlocking {
            launch { handlerCollections.add(TaskPoolManager()) }
            launch { handlerCollections.add(Linkage()) }
            launch { handlerCollections.add(InputListener()) }
            launch { handlerCollections.add(TaskTicker()) }
        }

        private fun runDispatcher() = runBlocking {
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

        private suspend fun handleEvent(event: EventType, args: Any) {
            when (event) {
                MEvent.Exit -> {
                    channel.send(when (args) {
                        is String -> args.toIntOrNull() ?: 0
                        is Int -> args
                        else -> args.toString().toIntOrNull() ?: 0
                    })
                    channel.close()
                }
            }
        }

        private fun postClean() {
            handlerCollections.forEach {
                it.close()
            }
        }
    }
}
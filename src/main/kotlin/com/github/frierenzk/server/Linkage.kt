package com.github.frierenzk.server

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.task.TaskStatus
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import java.net.BindException
import java.nio.file.Path
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

@ObsoleteCoroutinesApi
class Linkage: DispatcherBase() {
    override val eventMonitor = setOf(ServerEvent::class.java)
    private var port by Delegates.notNull<Int>()
    private lateinit var server: SocketIOServer

    override fun receiveEvent(event: EventType, args: Any) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (event) {
            is ServerEvent -> when (event) {
                ServerEvent.Default -> println("$event shouldn't be used")
                ServerEvent.AddTask -> if (args is Pair<*, *>) completionNotify(args, event)
                ServerEvent.StopTask -> if (args is Pair<*, *>) completionNotify(args, event)
                ServerEvent.AvailableList -> if (args is Pair<*, *>) sendList(args, event)
                ServerEvent.WorkingList -> if (args is Pair<*, *>) sendList(args, event)
                ServerEvent.WaitingList -> if (args is Pair<*, *>) sendList(args, event)
                ServerEvent.Status -> broadCastStatus(args)
                ServerEvent.BroadCast -> broadCast(args)
                else -> println(event)
            }
        }
    }

    private fun completionNotify(args: Pair<*, *>, event: ServerEvent) {
        val (uuid, name) = args
        if (uuid is UUID && name is String) {
            when (event) {
                ServerEvent.AddTask ->
                    server.getClient(uuid)?.sendEvent("add_task", "Add $name done")
                ServerEvent.StopTask ->
                    server.getClient(uuid)?.sendEvent("stop_task", "Stop $name done")
                else -> return
            }
        }
    }

    private fun sendList(args: Pair<*, *>, event: ServerEvent) {
        val (uuid, list) = args
        if (uuid is UUID) {
            val gson = GsonBuilder().apply { setPrettyPrinting() }.create()!!
            val data = when (list) {
                null -> return
                else -> gson.toJson(list, list::class.java)!!
            }
            when (event) {
                ServerEvent.AvailableList -> {
                    server.getClient(uuid)?.sendEvent("available_list", data)
                }
                ServerEvent.WorkingList -> {
                    server.getClient(uuid)?.sendEvent("processing_list", data)
                }
                ServerEvent.WaitingList -> {
                    server.getClient(uuid)?.sendEvent("waiting_list", data)
                }
                else -> return
            }
        }
    }

    private fun broadCastStatus(args: Any) = runBlocking {
        if (args is Pair<*, *>) {
            val (name, status) = args
            val gson = GsonBuilder().setPrettyPrinting().create()!!
            val data = gson.toJson(
                    mapOf(
                            "task" to name,
                            "state" to status.toString(),
                            "msg" to status.toString()
                    )
            )!!
            if (name is String && status is TaskStatus) {
                server.broadcastOperations
                        ?.sendEvent("broadcast_task_status_change", data)
                if (status.isFinished())
                    server.broadcastOperations
                            ?.sendEvent("broadcast_task_finish", data)
            }
        }
    }

    private fun broadCast(args: Any) = runBlocking {
        if (args is Pair<*, *>) {
            val (name, msg) = args
            val gson = GsonBuilder().apply { setPrettyPrinting() }.create()!!
            if (name is String && msg is String) {
                val data = gson.toJson(mapOf("task" to name, "broadcast_logs" to msg))!!
                server.broadcastOperations?.sendEvent("broadcast_logs", data)
            }
        }
    }

    private fun loadConfig() {
        val file = Path.of("server_settings.json").toFile()
        if (!(file.exists() && file.isFile)) {
            port = 21518
            return
        }
        val reader = JsonReader(file.reader())
        val gson = GsonBuilder().apply { setPrettyPrinting() }.create()!!
        val paras: HashMap<String, Any> = gson.fromJson(reader, HashMap<String, Any>()::class.java)
        port = paras.getOrDefault("port", 21518) as Int
    }

    private fun runServer() {
        val config = Configuration()
        config.hostname = "0.0.0.0"
        config.port = port
        server = SocketIOServer(config)
        server.addConnectListener { client ->
            runBlocking {
                println("New connection ${client.sessionId}")
                raiseEvent(PoolEvent.AvailableList, client.sessionId)
            }
        }
        server.addDisconnectListener { client ->
            println("${client.sessionId} disconnected")
        }
        server.addEventListener("set_add_task", String::class.java) { client, data, _ ->
            runBlocking {
                raiseEvent(PoolEvent.AddTask, Pair(client.sessionId, data))
            }
        }
        server.addEventListener("set_stop_task", String::class.java) { client, data, _ ->
            runBlocking {
                raiseEvent(PoolEvent.StopTask, Pair(client.sessionId, data))
            }
        }
        server.addEventListener("get_waiting_list", Any::class.java) { client, _, _ ->
            runBlocking {
                raiseEvent(PoolEvent.WaitingList, client.sessionId)
            }
        }
        server.addEventListener("get_processing_list", Any::class.java) { client, _, _ ->
            runBlocking {
                raiseEvent(PoolEvent.WorkingList, client.sessionId)
            }
        }
        server.addEventListener("get_available_list", Any::class.java) { client, _, _ ->
            runBlocking {
                raiseEvent(PoolEvent.AvailableList, client.sessionId)
            }
        }
        server.addEventListener("reload_config", Any::class.java) { client, _, _ ->
            runBlocking {
                raiseEvent(PoolEvent.ReloadConfig, client.sessionId)
            }
        }
        server.addEventListener("set_create_task", String::class.java) { client, data, _ ->
            runBlocking {
                val args = hashMapOf<String, Any>()
                JsonParser.parseString(data)?.asJsonObject?.entrySet()?.forEach { (key, value) ->
                    val str = value.asString
                    if (key is String && str is String) {
                        args[key] = str
                    }
                }
                args["uuid"] = client.sessionId
                raiseEvent(PoolEvent.CreateTask, args)
            }
        }
        server.start()
    }

    override fun closeEvent() {
        if (this::server.isInitialized) server.stop()
        super.closeEvent()
    }

    init {
        loadConfig()
        try {
            runServer()
        } catch (exception: BindException) {
            println(exception.message)
            println("Server closed")
        }
    }
}
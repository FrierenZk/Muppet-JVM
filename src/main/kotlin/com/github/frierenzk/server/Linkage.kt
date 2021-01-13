package com.github.frierenzk.server

import com.corundumstudio.socketio.AckMode
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.ticker.TickerEvent
import com.github.frierenzk.utils.ConfigOperator
import com.github.frierenzk.utils.ConfigOperator.projectGson
import com.github.frierenzk.utils.TypeUtils.castIntoJsonArray
import com.github.frierenzk.utils.TypeUtils.castIntoJsonObject
import com.github.frierenzk.utils.TypeUtils.castJsonPrimitive
import com.github.frierenzk.utils.TypeUtils.isJsonArrayOrObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.net.BindException
import java.util.*

@ObsoleteCoroutinesApi
class Linkage : DispatcherBase() {
    override val eventMonitor = setOf(ServerEvent::class.java)
    private lateinit var server: SocketIOServer
    private val listenerContext = newSingleThreadContext("${this::class.simpleName}/listener")

    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (event) {
            is ServerEvent -> when (event) {
                ServerEvent.Default -> println("$event shouldn't be used")
                ServerEvent.UpdateList -> broadCastUpdateList()
                ServerEvent.BroadCast ->
                    args.runIf(args.isDataPipe<Pair<String, String>>()) { broadCast(args.asDataPipe()!!) }
                ServerEvent.TaskFinish ->
                    args.runIf(args.isDataPipe<String>()) { broadCastFinish(args.asDataPipe()!!) }
                else -> println(event)
            }
        }
    }

    private fun broadCastUpdateList() {
        server.broadcastOperations?.sendEvent("update_available_list")
    }

    private fun broadCastFinish(args: Pipe<String, Unit>) {
        server.broadcastOperations?.sendEvent("task_finish", args.data)
    }

    private fun broadCast(args: Pipe<Pair<String, String>, Unit>) {
        server.broadcastOperations?.sendEvent("broadcast_logs", JsonArray().apply {
            this.add(args.data.first)
            this.add(args.data.second)
        })
    }

    private fun runServer() {
        val config = Configuration()
        config.ackMode = AckMode.MANUAL
        config.hostname = "0.0.0.0"
        config.port = ConfigOperator.loadServerConfig().get("port").asInt
        server = SocketIOServer(config)
        server.addConnectListener { client ->
            println("New connection ${client.sessionId}")
        }
        server.addDisconnectListener { client ->
            println("${client.sessionId} disconnected")
        }
        server.addEventListener("set_add_task", String::class.java) { _, data, ack ->
            val dataMap = if (isJsonArrayOrObject(data)) {
                val jsonObject = castIntoJsonObject(JsonParser.parseString(data))
                val map = hashMapOf<String, Any>()
                jsonObject.entrySet()
                    .forEach { (key, value) -> if (value is JsonPrimitive) map[key] = castJsonPrimitive(value) }
                map
            } else hashMapOf("name" to data)
            val pipe = Pipe<HashMap<String, out Any>, String>(dataMap) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.AddTask, pipe) }
        }
        server.addEventListener("set_stop_task", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.StopTask, pipe) }
        }
        server.addEventListener("get_waiting_list", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<List<String>> {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.WaitingList, pipe) }
        }
        server.addEventListener("get_processing_list", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<List<String>> {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.WorkingList, pipe) }
        }
        server.addEventListener("get_available_list", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<Map<String, String>> {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.AvailableList, pipe) }
        }
        server.addEventListener("reload_config", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<String> {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.ReloadConfig, pipe) }
        }
        server.addEventListener("set_create_task", String::class.java) { _, data, ack ->
            val map = hashMapOf<String, Any>()
            if (isJsonArrayOrObject(data)) {
                val jsonObject = castIntoJsonObject(JsonParser.parseString(data))
                jsonObject.entrySet().forEach { (key, value) ->
                    if (value is JsonPrimitive) map[key] = castJsonPrimitive(value)
                }
            }
            val pipe = Pipe<HashMap<String, out Any>, String>(map) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.CreateTask, pipe) }
        }
        server.addEventListener("get_task_status", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.TaskStatus, pipe) }
        }

        server.addEventListener("reset_ticker", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<String> {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(TickerEvent.Reset, pipe) }
        }
        server.addEventListener("enable_ticker", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(TickerEvent.Enable, pipe) }
        }
        server.addEventListener("disable_ticker", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(TickerEvent.Disable, pipe) }
        }
        server.addEventListener("add_timer", String::class.java) { _, data, ack ->
            val jsonObject = castIntoJsonObject(JsonParser.parseString(data))
            val map = hashMapOf<String, Any>()
            jsonObject.entrySet().forEach { (key, value) ->
                if (value is JsonPrimitive) map[key] = castJsonPrimitive(value)
            }
            val pipe = Pipe<Map<String, Any>, String>(map) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(TickerEvent.AddTimer, pipe) }
        }
        server.addEventListener("modify_interval", String::class.java) { _, data, ack ->
            val jsonArray = castIntoJsonArray(JsonParser.parseString(data))
            if (jsonArray.size() < 2) {
                ack.sendAckData("Invalid paras")
                return@addEventListener
            }
            val name = jsonArray[0]?.asString ?: ""
            val interval = jsonArray[1]?.asInt ?: -1
            if (name.isBlank() || interval < 1) {
                ack.sendAckData("Invalid paras with name = $name, interval = $interval")
                println("ack back")
                return@addEventListener
            }
            val pipe = Pipe<Pair<String, Number>, String>(name to interval) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(TickerEvent.ModifyInterval, pipe) }
        }
        server.start()
    }

    override fun closeEvent() {
        if (this::server.isInitialized) server.stop()
        super.closeEvent()
    }

    override fun init() {
        try {
            runServer()
        } catch (exception: BindException) {
            exception.printStackTrace()
            println("Server closed because of $exception")
        }
    }
}
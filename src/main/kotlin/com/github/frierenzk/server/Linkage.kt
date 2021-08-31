package com.github.frierenzk.server

import com.corundumstudio.socketio.AckMode
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.github.frierenzk.config.ConfigEvent
import com.github.frierenzk.config.ConfigOperator
import com.github.frierenzk.config.ConfigOperator.projectGson
import com.github.frierenzk.config.IncompleteBuildConfig
import com.github.frierenzk.config.IncompleteTickerConfig
import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.task.PoolEvent
import com.github.frierenzk.ticker.TickerConfig
import com.github.frierenzk.utils.TypeUtils.castIntoJsonObject
import com.github.frierenzk.utils.TypeUtils.isJsonArrayOrObject
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.net.BindException

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
                    args.runIf(args.isDataPipe<JsonObject>()) { broadCast(args.asDataPipe()!!) }
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

    private fun broadCast(args: Pipe<JsonObject, Unit>) {
        server.broadcastOperations?.sendEvent("broadcast_logs", args.data)
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
            var conf: IncompleteBuildConfig? = null
            val name: String = if (isJsonArrayOrObject(data)) {
                val jsonObject = castIntoJsonObject(JsonParser.parseString(data))
                try {
                    conf = projectGson.fromJson(jsonObject.get("config"), IncompleteBuildConfig::class.java)
                    jsonObject.get("name").asString
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    ""
                }
            } else data
            scope.launch(listenerContext) {
                raiseEvent(ConfigEvent.GetConfig, Pipe<String, BuildConfig>(name) {
                    runBlocking {
                        raiseEvent(PoolEvent.CreateTask,
                            Pipe<BuildConfig, String>(it.let { if (conf is IncompleteBuildConfig) it + conf else it })
                            { ret -> ack.sendAckData(ret) })
                    }
                })
            }
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
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.GetConfigList, pipe) }
        }
        server.addEventListener("reload_config", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<String> {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.Reload, pipe) }
        }
        server.addEventListener("get_task_status", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.TaskStatus, pipe) }
        }
        server.addEventListener("get_task_config", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, BuildConfig>(data) {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.GetConfig, pipe) }
        }
        server.addEventListener("set_add_config", String::class.java) { _, data, ack ->
            try {
                projectGson.fromJson(data, IncompleteBuildConfig::class.java)
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }?.toConf().let { conf ->
                if (conf is BuildConfig) scope.launch(listenerContext) {
                    raiseEvent(ConfigEvent.AddConfig, Pipe<BuildConfig, String>(conf) { ack.sendAckData(it) })
                } else ack.sendAckData("Invalid data received $data")
            }
        }
        server.addEventListener("set_change_config", String::class.java) { _, data, ack ->
            val conf = try {
                projectGson.fromJson(data, IncompleteBuildConfig::class.java)
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
            if (conf is IncompleteBuildConfig && !conf.isEmpty()) scope.launch(listenerContext) {
                raiseEvent(ConfigEvent.ModifyConfig, Pipe<IncompleteBuildConfig, String>(conf) { ack.sendAckData(it) })
            } else ack.sendAckData("Invalid data received $data")
        }
        server.addEventListener("set_delete_config", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.DeleteConfig, pipe) }
        }
        server.addEventListener("get_timer_config", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, TickerConfig?>(data) {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.GetTicker, pipe) }
        }
        server.addEventListener("add_ticker", String::class.java) { _, data, ack ->
            try {
                projectGson.fromJson(data, IncompleteTickerConfig::class.java)
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }?.toConf().let { conf ->
                if (conf is TickerConfig) scope.launch(listenerContext) {
                    raiseEvent(ConfigEvent.AddTicker, Pipe<TickerConfig, String>(conf) { ack.sendAckData(it) })
                } else ack.sendAckData("Invalid data received $data")
            }
        }
        server.addEventListener("modify_ticker", String::class.java) { _, data, ack ->
            val conf = try {
                projectGson.fromJson(data, IncompleteTickerConfig::class.java)
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
            if (conf is IncompleteTickerConfig && !conf.isEmpty()) scope.launch(listenerContext) {
                raiseEvent(ConfigEvent.ModifyConfig, Pipe<IncompleteTickerConfig, String>(conf) { ack.sendAckData(it) })
            } else ack.sendAckData("Invalid data received $data")
        }
        server.addEventListener("delete_ticker", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.DeleteTicker, pipe) }
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
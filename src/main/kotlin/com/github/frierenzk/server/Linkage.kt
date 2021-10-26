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
                ServerEvent.BroadCast ->
                    args.runIf(args.isDataPipe<JsonObject>()) { broadCast(args.asDataPipe()!!) }
                ServerEvent.TaskFinish ->
                    args.runIf(args.isDataPipe<String>()) { broadCastFinish(args.asDataPipe()!!) }
                else -> println(event)
            }
        }
    }

    private fun broadCastFinish(args: Pipe<String, Unit>) {
        server.broadcastOperations?.sendEvent("task_finish", args.data)
    }

    private fun broadCast(args: Pipe<JsonObject, Unit>) {
        server.broadcastOperations?.sendEvent("broadcast_logs", args.data.toString())
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
        //Config
        server.addEventListener("get_config_list", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<Map<String, String>> {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.GetConfigList, pipe) }
        }
        server.addEventListener("get_config", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, BuildConfig>(data) {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.GetConfig, pipe) }
        }
        server.addEventListener("get_relative_config", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, List<String>>(data) {
                ack.sendAckData(projectGson.toJson(it))
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.GetRelativeConfig, pipe) }
        }
        server.addEventListener("reload_config", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<String> {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.Reload, pipe) }
        }
        server.addEventListener("add_config", String::class.java) { _, data, ack ->
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
        server.addEventListener("modify_config", String::class.java) { _, data, ack ->
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
        server.addEventListener("delete_config", String::class.java) { _, data, ack ->
            val pipe = Pipe<String, String>(data) {
                ack.sendAckData(it)
            }
            scope.launch(listenerContext) { raiseEvent(ConfigEvent.DeleteConfig, pipe) }
        }
        server.addEventListener("add_task", String::class.java) { _, data, ack ->
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
                raiseEvent(ConfigEvent.GetConfig, Pipe<String, BuildConfig?>(name) {
                    runBlocking {
                        val con = if (it is BuildConfig) {
                            if (conf is IncompleteBuildConfig) it + conf
                            else it
                        } else conf?.toConf()
                        if (con is BuildConfig) raiseEvent(PoolEvent.CreateTask,
                            Pipe<BuildConfig, String>(con) { ret -> ack.sendAckData(ret) })
                        else ack.sendAckData("Invalid Parameters received")
                    }
                })
            }
        }
        //Pool
        server.addEventListener("get_task_list", Any::class.java) { _, _, ack ->
            val pipe = Pipe.callback<List<Int>> { ack.sendAckData(projectGson.toJson(it)) }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.ProcessingList, pipe) }
        }
        server.addEventListener("stop_task", Int::class.java) { _, data, ack ->
            val pipe = Pipe<Int, String>(data) { ack.sendAckData(it) }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.StopTask, pipe) }
        }
        server.addEventListener("get_task_status", Int::class.java) { _, data, ack ->
            val pipe = Pipe<Int, String>(data) { ack.sendAckData(it) }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.GetTaskStatus, pipe) }
        }
        server.addEventListener("get_task_name", Int::class.java) { _, data, ack ->
            val pipe = Pipe<Int, String>(data) { ack.sendAckData(it) }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.GetTaskName, pipe) }
        }
        server.addEventListener("get_task_config", Int::class.java) { _, data, ack ->
            val pipe = Pipe<Int, BuildConfig>(data) { ack.sendAckData(projectGson.toJson(it)) }
            scope.launch(listenerContext) { raiseEvent(PoolEvent.GetTaskConfig, pipe) }
        }
        //Ticker
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
package com.github.frierenzk.config

import com.github.frierenzk.dispatcher.DispatcherBase
import com.github.frierenzk.dispatcher.EventType
import com.github.frierenzk.dispatcher.Pipe
import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.ticker.TickerConfig
import com.github.frierenzk.ticker.TickerEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

@ObsoleteCoroutinesApi
class ConfigCenter : DispatcherBase() {
    override val eventMonitor by lazy { setOf(ConfigEvent::class.java) }
    private val configContext by lazy { newSingleThreadContext("config") }
    private val ticker by lazy { ticker(delayMillis = 60 * 1000L) }
    private val channel by lazy { Channel<Boolean>(1) }
    private val buildList by lazy { ConcurrentHashMap<String, BuildConfig>() }
    private val tickers by lazy { ConcurrentHashMap<String, TickerConfig>() }
    private var manually = true

    override fun receiveEvent(event: EventType, args: Pipe<*, *>) {
        when (event) {
            is ConfigEvent -> when (event) {
                ConfigEvent.Default -> println("$event shouldn't be used")
                ConfigEvent.Reload -> args.runIf(args.isPipe<Any, String>()) { reload(args.asPipe()!!) }
                ConfigEvent.Save -> args.runIf(args.isCallbackPipe<String>()) { save(args.asCallbackPipe()!!) }
                ConfigEvent.GetConfig -> args.runIf(args.isPipe<String, BuildConfig?>()) { getBuildConfig(args.asPipe()!!) }
                ConfigEvent.GetConfigList ->
                    args.runIf(args.isCallbackPipe<Map<String, String>>()) { getBuildConfigList(args.asPipe()!!) }
                ConfigEvent.GetRelativeConfig ->
                    args.runIf(args.isPipe<String, List<String>>()) { getRelativeConfigList(args.asPipe()!!) }
                ConfigEvent.AddConfig -> args.runIf(args.isPipe<BuildConfig, String>()) { addBuildConfig(args.asPipe()!!) }
                ConfigEvent.ModifyConfig -> args.runIf(args.isPipe<IncompleteBuildConfig, String>()) {
                    modifyBuildConfig(args.asPipe()!!)
                }
                ConfigEvent.DeleteConfig -> args.runIf(args.isPipe<String, String>()) { deleteBuildConfig(args.asPipe()!!) }
                ConfigEvent.GetTickerList -> args.runIf(args.isCallbackPipe<List<String>>()) { getTickerList(args.asPipe()!!) }
                ConfigEvent.GetTicker -> args.runIf(args.isPipe<String, TickerConfig?>()) { getTicker(args.asPipe()!!) }
                ConfigEvent.AddTicker -> args.runIf(args.isPipe<TickerConfig, String>()) { addTicker(args.asPipe()!!) }
                ConfigEvent.ModifyTicker -> args.runIf(args.isPipe<IncompleteTickerConfig, String>()) {
                    modifyTicker(args.asPipe()!!)
                }
                ConfigEvent.DeleteTicker -> args.runIf(args.isPipe<String, String>()) { deleteTicker(args.asPipe()!!) }
            }
        }
        super.receiveEvent(event, args)
    }

    private fun getBuildConfig(args: Pipe<String, BuildConfig?>) {
        args.callback(buildList[args.data]?.deepCopy())
    }

    private fun getBuildConfigList(args: Pipe<Unit, Map<String, String>>) {
        val map = HashMap<String, String>()
        map.putAll(buildList.map { Pair(it.key, it.value.category) })
        args.callback(map)
    }

    private fun getRelativeConfigList(args: Pipe<String, List<String>>) {
        args.callback(buildList.filter {
            it.value.name.contains(args.data) || it.value.category.contains(args.data)
                    || it.value.profile.contains(args.data) || it.value.projectDir?.contains(args.data) ?: false
                    || it.value.extraParas.filterValues { it.toString().contains(args.data) }.isNotEmpty()
        }.map { it.value.name })
    }

    private fun addBuildConfig(args: Pipe<BuildConfig, String>) {
        if (buildList.containsKey(args.data.name)) {
            args.callback("Config name duplicated")
        } else {
            buildList[args.data.name] = args.data
            ConfigOperator.saveBuildList(HashMap(buildList))
            args.callback("Success")
        }
    }

    private fun modifyBuildConfig(args: Pipe<IncompleteBuildConfig, String>) = args.data.name.run {
        if (this is String) buildList[this].let {
            if (it is BuildConfig) try {
                buildList[this] = it + args.data
                ConfigOperator.saveBuildList(HashMap(buildList))
                args.callback("Success")
            } catch (exception: InvalidParameterException) {
                exception.printStackTrace()
                args.callback(exception.stackTraceToString())
            }
            else args.callback("Can not find target config")
        }
        else args.callback("Invalid values")
    }


    private fun deleteBuildConfig(args: Pipe<String, String>) {
        if (buildList.containsKey(args.data)) {
            buildList.remove(args.data)
            ConfigOperator.saveBuildList(HashMap(buildList))
            args.callback("Success")
        } else args.callback("Can not find target config")
    }

    private fun getTickerList(args: Pipe<Unit, List<String>>) {
        args.callback(tickers.keys().toList())
    }

    private fun getTicker(args: Pipe<String, TickerConfig?>) {
        args.callback(tickers[args.data])
    }

    private fun addTicker(args: Pipe<TickerConfig, String>) {
        if (tickers.containsKey(args.data.name)) {
            args.callback("Config name duplicated")
        } else {
            tickers[args.data.name] = args.data
            ConfigOperator.saveTickerConfig(HashMap(tickers))
            args.callback("Success")
        }
    }

    private fun modifyTicker(args: Pipe<IncompleteTickerConfig, String>) = args.data.name.run {
        if (this is String) tickers[this].let {
            if (it is TickerConfig) try {
                tickers[this] = it + args.data
                ConfigOperator.saveTickerConfig(HashMap(tickers))
                args.callback("Success")
            } catch (exception: InvalidParameterException) {
                exception.printStackTrace()
                args.callback(exception.stackTraceToString())
            } else args.callback("Can not find target config")
        } else args.callback("Invalid values")
    }

    private fun deleteTicker(args: Pipe<String, String>) {
        if (tickers.containsKey(args.data)) {
            tickers.remove(args.data)
            ConfigOperator.saveTickerConfig(HashMap(tickers))
            args.callback("Success")
        } else args.callback("Can not find target config")
    }

    private fun reload(args: Pipe<Any?, String>) {
        val result = channel.trySend(args.data == true)
        if (result.isFailure) args.callback("Failure")
        else if (result.isClosed) args.callback("Closed")
        else if (result.isSuccess) args.callback("Success").also { manually = true }
    }

    private fun save(args: Pipe<Unit, String>) {
        ConfigOperator.saveBuildList(HashMap(buildList))
        ConfigOperator.saveTickerConfig(HashMap(tickers))
        args.callback("Success")
    }

    private fun updateList() {
        buildList.clear()
        buildList.putAll(ConfigOperator.loadBuildList())
    }

    private fun updateTicker() {
        tickers.clear()
        tickers.putAll(ConfigOperator.loadTickerConfig())
        val t = manually
        val pipe = Pipe<Map<String, TickerConfig>, String>(tickers) {
            if (t) println("Ticker update $it")
        }
        runBlocking { raiseEvent(TickerEvent.Update, pipe) }
    }

    override fun init() {
        val update = fun(force: Boolean) {
            if (!ConfigOperator.checkBuildList() || force) updateList()
            else if (manually) println("Found no change in task")

            if (!ConfigOperator.checkTickerConfig() || force) updateTicker()
            else if (manually) println("Found no change in ticker")

            if (manually) println("Config load done")

            manually = false
        }
        scope.launch(configContext) {
            while (true) {
                select<Unit> {
                    ticker.onReceive {
                        update(false)
                    }
                    channel.onReceive {
                        update(it)
                    }
                }
            }
        }
        runBlocking { channel.send(true) }
    }
}
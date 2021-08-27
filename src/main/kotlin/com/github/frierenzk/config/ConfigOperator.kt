package com.github.frierenzk.config

import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.ticker.TickerConfig
import com.github.frierenzk.ticker.TickerConfigAdaptor
import com.github.frierenzk.utils.TypeUtils.castIntoJsonObject
import com.google.gson.*
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object ConfigOperator {
    val projectGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(TickerConfig::class.java, TickerConfigAdaptor())
            .setPrettyPrinting().create()
    }
    private val stamps by lazy { HashMap<ConfigFile, Long>() }
    private fun loadFile(file: ConfigFile): JsonElement = file.read {
        return try {
            if (file.exists()) {
                stamps[file] = file.stamp()
                JsonParser.parseReader(file.reader())
            } else {
                file.createNewFile()
                file.bufferedWriter().run {
                    this.write(JsonObject().toString())
                    this.flush()
                    this.close()
                }
                stamps[file] = file.stamp()
                JsonNull.INSTANCE
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            stamps[file] = 0
            JsonNull.INSTANCE
        }
    }

    private fun saveFile(file: ConfigFile, data: JsonElement) = file.write {
        if (file.exists()) file.delete()
        file.createNewFile()
        file.bufferedWriter().run {
            this.write(projectGson.toJson(data).let {
                println("File ${file.name} saved ${it.length} Bytes")
                it
            })
            this.flush()
            this.close()
        }
        this.stamps[file] = file.stamp()
    }

    private class ConfigFile(fileName: String) : File(fileName) {
        private val lock by lazy { ReentrantReadWriteLock() }
        inline fun <T> read(action: () -> T): T = lock.read(action)
        inline fun <T> write(action: () -> T): T = lock.write(action)
        fun stamp() = read { lastModified().xor(length().shl(32) * 31) }
    }

    private val serverFile by lazy { ConfigFile("server_settings.json") }
    private val timerFile by lazy { ConfigFile("timer.json") }
    private val buildConfigFile by lazy { ConfigFile("build_configs.json") }
    private val buildListFile by lazy { ConfigFile("build_list.json") }

    fun loadServerConfig(): JsonObject {
        val jsonObject = castIntoJsonObject(loadFile(serverFile))
        if (!jsonObject.has("port")) jsonObject.addProperty("port", 21518)
        return jsonObject
    }

    fun loadTickerConfig(): Map<String, TickerConfig> {
        val jsonObject = castIntoJsonObject(loadFile(timerFile))
        val ret = hashMapOf<String, TickerConfig>()
        jsonObject.entrySet().forEach { (name, data) ->
            try {
                projectGson.fromJson(data, IncompleteTickerConfig::class.java).toConf().let {
                    if (it is TickerConfig) ret[it.name] = it
                    else println("Invalid config data[$name]: $data")
                }
            } catch (exception: JsonSyntaxException) {
                println("Invalid config data[$name]: $data")
                exception.printStackTrace()
            }
        }
        return ret
    }

    fun saveTickerConfig(configs: Map<String, TickerConfig>) {
        saveFile(timerFile, projectGson.toJsonTree(configs.toSortedMap()))
    }

    fun checkTickerConfig(): Boolean = stamps[timerFile] == timerFile.stamp()

    fun loadBuildConfigs(): JsonObject {
        return castIntoJsonObject(loadFile(buildConfigFile))
    }

    fun saveBuildConfigs(jsonObject: JsonObject) {
        saveFile(buildConfigFile, jsonObject)
    }

    fun loadBuildList(): Map<String, BuildConfig> {
        val configs = hashMapOf<String, BuildConfig>()
        val jsonObject = castIntoJsonObject(loadFile(buildListFile))
        jsonObject.entrySet().forEach { (_, jsonElement) ->
            val obj = castIntoJsonObject(jsonElement)
            obj.entrySet().forEach { (name, data) ->
                try {
                    projectGson.fromJson(data, IncompleteBuildConfig::class.java).toConf().let {
                        if (it is BuildConfig) configs[it.name] = it
                        else println("Invalid config data[$name]: $data")
                    }
                } catch (exception: JsonSyntaxException) {
                    println("Invalid config data[$name]: $data")
                    exception.printStackTrace()
                }
            }
        }
        val check = hashMapOf<String, String>()
        configs.forEach { (_, value) ->
            val source = value.getSource()
            if (check.containsKey(source)) {
                if (value.projectDir == null) {
                    println(
                        "[Warning][BuildList]Task ${value.name} sources path is same as ${check[source]}, " +
                                "please make sure their have same projectDir to avoid crash when parallel working"
                    )
                }
            } else {
                check[source] = value.name
            }
        }
        return configs
    }

    fun saveBuildList(configs: Map<String, BuildConfig>) {
        val map = hashMapOf<String, HashMap<String, BuildConfig>>()
        configs.forEach { (_, conf) ->
            map.getOrPut(conf.category) { hashMapOf() }[conf.name] = conf
        }
        val sort = HashMap<String, JsonElement>()
        map.forEach { (key, value) -> sort[key] = projectGson.toJsonTree(value.toSortedMap()) }
        saveFile(buildListFile, projectGson.toJsonTree(sort.toSortedMap()))
    }

    fun checkBuildList(): Boolean = stamps[buildListFile] == buildListFile.stamp()
}
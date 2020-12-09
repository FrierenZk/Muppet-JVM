package com.github.frierenzk.utils

import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.utils.TypeUtils.castIntoJsonObject
import com.github.frierenzk.utils.TypeUtils.castJsonPrimitive
import com.google.gson.*
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object ConfigOperator {
    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    private fun loadFile(file: ConfigFile): JsonElement = file.read {
        return try {
            if (file.exists())
                JsonParser.parseReader(file.reader())
            else {
                file.createNewFile()
                file.bufferedWriter().run {
                    this.write(JsonObject().toString())
                    this.flush()
                    this.close()
                }
                JsonNull.INSTANCE
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            JsonNull.INSTANCE
        }
    }

    private fun saveFile(file: ConfigFile, data: JsonElement) = file.write {
        if (file.exists()) file.delete()
        file.createNewFile()
        file.bufferedWriter().run {
            this.write(gson.toJson(data).let {
                println("File ${file.name} saved ${it.length} Bytes")
                it
            })
            this.flush()
            this.close()
        }
    }

    private class ConfigFile(fileName: String) : File(fileName) {
        private val lock by lazy { ReentrantReadWriteLock() }
        inline fun <T> read(action: () -> T): T = lock.read(action)
        inline fun <T> write(action: () -> T): T = lock.write(action)
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

    fun loadTickerConfig(): HashMap<String, HashMap<String, Any>> {
        val jsonObject = castIntoJsonObject(loadFile(timerFile))
        val ret = hashMapOf<String, HashMap<String, Any>>()
        jsonObject.entrySet().forEach { (name, subObj) ->
            if (subObj.isJsonObject) subObj.asJsonObject.entrySet().forEach { (key, value) ->
                if (value.isJsonPrimitive) ret.getOrPut(name) { hashMapOf() }[key] =
                    castJsonPrimitive(value.asJsonPrimitive)
            }
        }
        return HashMap(ret.filter { it.value.containsKey("interval") })
    }

    fun loadBuildConfigs(): JsonObject {
        return castIntoJsonObject(loadFile(buildConfigFile))
    }

    fun saveBuildConfigs(jsonObject: JsonObject) {
        saveFile(buildConfigFile, jsonObject)
    }

    fun loadBuildList(): HashMap<String, BuildConfig> {
        val configs = hashMapOf<String, BuildConfig>()
        val jsonObject = castIntoJsonObject(loadFile(buildListFile))
        jsonObject.entrySet().forEach { (_, jsonElement) ->
            val obj = castIntoJsonObject(jsonElement)
            obj.entrySet().forEach { (_, data) ->
                val config: BuildConfig? = gson.fromJson(data, BuildConfig::class.java)
                if (config is BuildConfig) configs[config.name] = config
            }
        }
        return configs
    }

    fun saveBuildList(configs: HashMap<String, BuildConfig>) {
        val map = hashMapOf<String, HashMap<String, BuildConfig>>()
        configs.forEach { (_, conf) ->
            map.getOrPut(conf.category) { hashMapOf() }[conf.name] = conf
        }
        val sort = HashMap<String, JsonElement>()
        map.forEach { (key, value) -> sort[key] = gson.toJsonTree(value.toSortedMap()) }
        saveFile(buildListFile, gson.toJsonTree(sort.toSortedMap()))
    }
}
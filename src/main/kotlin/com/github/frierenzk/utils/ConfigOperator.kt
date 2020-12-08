package com.github.frierenzk.utils

import com.github.frierenzk.task.BuildConfig
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
        val jsonObject =
            loadFile(serverFile).takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        if (!jsonObject.has("port")) jsonObject.addProperty("port", 21518)
        return jsonObject
    }

    fun loadTickerConfig(): HashMap<String, HashMap<String, String>> {
        val jsonObject = loadFile(timerFile).takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        val ret = hashMapOf<String, HashMap<String, String>>()
        jsonObject.entrySet().forEach {
            (it.value.asJsonObject ?: JsonObject()).entrySet()?.forEach { (key, value) ->
                value.asString?.run { ret.getOrPut(it.key) { hashMapOf() }[key] = this }
            }
        }
        return HashMap(ret.filterNot { it.value.containsKey("interval") })
    }

    fun loadBuildConfigs(): JsonObject {
        return loadFile(buildConfigFile).takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
    }

    fun saveBuildConfigs(jsonObject: JsonObject) {
        saveFile(buildConfigFile, jsonObject)
    }

    fun loadBuildList(): HashMap<String, BuildConfig> {
        val configs = hashMapOf<String, BuildConfig>()
        val jsonObject =
            loadFile(buildListFile).takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        jsonObject.entrySet().forEach { (_, jsonElement) ->
            val obj = jsonElement.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
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
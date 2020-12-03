package com.github.frierenzk.task

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.nio.file.Path

class BuildConfig {
    companion object {
        private val map = sortedMapOf("version" to "0.1.2")
        private val uploadAddress by lazy { map["uploadAddress"]!! }
        fun loadBuildConfigs() {
            val file = Path.of("build_configs.json").toFile()

            if (!file.exists()) {
                file.createNewFile()
                file.writeBytes(GsonBuilder().setPrettyPrinting().create()!!.toJson(map)!!.toByteArray())
                throw IllegalArgumentException("Can not read upload server address from build_configs.json")
            }
            var rewrite = false
            JsonParser.parseReader(file.reader()).asJsonObject!!.entrySet()!!.filterNotNull()
                    .filterNot { it.key.isBlank() }.forEach { (key, element) ->
                        if (key == "version" && map[key] != element.asString) rewrite = true
                        map[key] = element.asString
                    }
            if (!map.containsKey("uploadAddress")) throw IllegalArgumentException("Can not read upload server address from build_configs.json")
            if (rewrite) {
                val writer = file.bufferedWriter()
                writer.write(GsonBuilder().setPrettyPrinting().create()!!.toJson(map)!!)
                writer.flush()
                writer.close()
            }
        }
    }
    lateinit var name: String
    lateinit var category: String
    lateinit var profile: String
    var projectDir: String? = null
    var sourcePath: String = "\$default/catv-hgu-sfu-allinone"
    var uploadPath: String = "\$default"
    var svnBasePath: String? = null
    var extraParas: HashMap<String, Any> = hashMapOf()

    fun getFullSourcePath(): String {
        val replaceRule: HashMap<String, String> = hashMapOf(
                "\$default" to "${Path.of("../..").toFile().toPath()}/$category/${projectDir ?: name}",
                "\$base" to "${Path.of("../..").toFile().toPath()}",
                "\$catv" to "${Path.of("../..").toFile().toPath()}",
                "\$category" to category,
                "\$name" to (projectDir ?: name),
                "\$projectDir" to (projectDir ?: name)
        )
        replaceRule.forEach { (key, value) ->
            if (sourcePath.contains(key))
                sourcePath = sourcePath.replace(key, value)
        }
        return sourcePath
    }

    fun getFullUploadPath(): String {
        val cat = when(category) {
            "tags" -> "tags_version"
            "branches" -> "branches_version"
            else -> category
        }
        val replaceRule: HashMap<String, String> = hashMapOf(
                "\$default" to "buildmanager@$uploadAddress:/volume1/version/$cat/${projectDir ?: name}",
                "\$base" to "buildmanager@$uploadAddress:/volume1/version",
                "\$version" to "buildmanager@$uploadAddress:/volume1/version",
                "\$category" to cat,
                "\$name" to (projectDir ?: name),
                "\$projectDir" to (projectDir ?: name)
        )
        replaceRule.forEach { (key, value) ->
            if (uploadPath.contains(key))
                uploadPath = uploadPath.replace(key, value)
        }
        return uploadPath
    }

    fun getFullSvnBasePath(): String {
        return if (svnBasePath is String) {
            val replaceRule: HashMap<String, String> = hashMapOf(
                    "\$default" to "${Path.of("../..").toFile().toPath()}/$category/${projectDir ?: name}",
                    "\$base" to "${Path.of("../..").toFile().toPath()}",
                    "\$catv" to "${Path.of("../..").toFile().toPath()}",
                    "\$source" to getFullSourcePath(),
                    "\$category" to category,
                    "\$name" to (projectDir ?: name),
                    "\$projectDir" to (projectDir ?: name)
            )
            replaceRule.forEach { (key, value) ->
                if (svnBasePath!!.contains(key))
                    svnBasePath = svnBasePath!!.replace(key, value)
            }
            svnBasePath!!
        } else getFullSourcePath()
    }

    fun copy(): BuildConfig = BuildConfig().let {
        if (this::name.isInitialized) it.name = this.name
        if (this::category.isInitialized) it.category = this.category
        if (this::profile.isInitialized) it.profile = this.profile

        it.projectDir = this.projectDir

        it.sourcePath = this.sourcePath
        it.uploadPath = this.uploadPath
        it.svnBasePath = this.svnBasePath

        it.extraParas = this.extraParas

        it
    }
}
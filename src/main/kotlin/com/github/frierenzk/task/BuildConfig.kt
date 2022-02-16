package com.github.frierenzk.task

import com.github.frierenzk.config.ConfigOperator
import com.github.frierenzk.config.IncompleteBuildConfig
import java.nio.file.Path

data class BuildConfig(
    val name: String,
    val category: String,
    val profile: String,
    val extraParas: HashMap<String, Any> = hashMapOf(),
) {
    companion object {
        val uploadAddress by lazy { configs.get("uploadAddress").asString!! }
        val uploadUser by lazy { configs.get("uploadUser")?.asString ?: "buildmanager" }
        val uploadPassword by lazy { configs.get("uploadPassword")?.asString ?: "654321" }
        private val configs by lazy {
            ConfigOperator.loadBuildConfigs().also {
                val version = this::class.java.`package`.implementationVersion
                if ((!it.has("version") || (it.getAsJsonPrimitive("version").asString != version))) {
                    it.addProperty("version", version)
                    ConfigOperator.saveBuildConfigs(it)
                    println(it)
                }
            }
        }
    }

    val projectDir: String? = extraParas["projectDir"].let { if (it is String) it else null }

    fun getSource(): String {
        var path = extraParas["source"].let { if (it is String) it else null } ?: "\${default}"
        mapOf(
            "default" to "${Path.of("../..").toFile().toPath()}/$category/${projectDir ?: name}",
            "base" to "${Path.of("../..").toFile().toPath()}",
            "category" to category,
            "name" to name,
            "projectDir" to (projectDir ?: name)
        ).forEach { path = path.replace("\${${it.key}}", it.value) }
        return path
    }

    fun getUpload(): String {
        var upload = extraParas["upload"].let { if (it is String) it else null } ?: "\${default}"
        mapOf(
            "default" to "$uploadUser@$uploadAddress:/volume1/version/$category/${projectDir ?: name}",
            "base" to "$uploadUser@$uploadAddress:/volume1/version",
            "version" to "$uploadUser@$uploadAddress:/volume1/version",
            "category" to category,
            "name" to name,
            "projectDir" to (projectDir ?: name)
        ).forEach { upload = upload.replace("\${${it.key}}", it.value) }
        return upload
    }

    fun getRemote() = extraParas["svn"].let { if (it is String) it else null }

    fun getLocal() = extraParas["local"].let { if (it is String) "${getSource()}/$it" else getSource() }

    fun getImage() =
        extraParas["imageDir"].let { if (it is String) "${getLocal()}/$it" else "${getLocal()}/Project/images" }

    operator fun plus(other: BuildConfig): BuildConfig {
        return this + IncompleteBuildConfig(other)
    }

    operator fun plus(other: IncompleteBuildConfig): BuildConfig {
        return (IncompleteBuildConfig(this) + other).toConf()!!
    }

    operator fun get(key: String): Any? {
        return extraParas[key]
    }

    operator fun set(key: String, value: Any) {
        extraParas[key] = value
    }

    fun conflicts(other: BuildConfig) = this.getSource() == other.getSource()

    fun isInvalid(): Boolean {
        for (i in arrayOf({ this.name == "" }, { this.category == "" }, { this.profile == "" })) if (i()) return true
        return false
    }

    fun deepCopy(
        name: String = this.name,
        category: String = this.category,
        profile: String = this.profile,
        extraParas: HashMap<String, Any> = HashMap(this.extraParas),
    ) = BuildConfig(name, category, profile, extraParas)
}
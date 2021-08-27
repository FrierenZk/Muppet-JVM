package com.github.frierenzk.config

import com.github.frierenzk.task.BuildConfig
import java.security.InvalidParameterException

data class IncompleteBuildConfig(
    val name: String? = null,
    val category: String? = null,
    val profile: String? = null,
    val extraParas: HashMap<String, Any> = HashMap(),
) {
    constructor(config: BuildConfig) : this(config.name, config.category, config.profile, config.extraParas)

    operator fun plus(other: IncompleteBuildConfig): IncompleteBuildConfig {
        return IncompleteBuildConfig(
            other.name ?: name,
            other.category ?: category,
            other.profile ?: profile,
            HashMap(this.extraParas.plus(other.extraParas))
        ).let { if (it.isEmpty()) throw InvalidParameterException(other.toString()) else it }
    }

    operator fun plus(other: BuildConfig): BuildConfig {
        val config = (this + IncompleteBuildConfig(other)).toConf()
        if (config is BuildConfig) return config
        else throw InvalidParameterException(other.toString())
    }

    fun toConf(): BuildConfig? {
        return if (name is String && category is String && profile is String)
            BuildConfig(name, category, profile, extraParas).let { if (it.isInvalid()) null else it }
        else null
    }

    fun isEmpty(): Boolean {
        for (i in arrayOf(
            { this.name == null || this.name == "" },
            { this.category == null || this.category == "" },
            { this.profile == null || this.category == "" },
            { this.extraParas.filterNot { it.value == "" || it.value == Unit }.isEmpty() }
        )) if (!i()) return false
        return true
    }
}
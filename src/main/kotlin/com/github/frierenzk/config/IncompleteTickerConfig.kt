package com.github.frierenzk.config

import com.github.frierenzk.task.BuildConfig
import com.github.frierenzk.ticker.TickerConfig
import java.security.InvalidParameterException

data class IncompleteTickerConfig(
    val name: String? = null,
    val delay: Int? = null,
    val ref: String? = null,
    val config: IncompleteBuildConfig? = null,
) {
    constructor(other: TickerConfig) : this(other.name, other.delay, other.let {
        when (it) {
            is TickerConfig.Ref -> it.ref
            is TickerConfig.Mix -> it.ref
            else -> null
        }
    }, other.let {
        when (it) {
            is TickerConfig.Mix -> it.config
            is TickerConfig.Config -> IncompleteBuildConfig(it.config)
            else -> null
        }
    })

    operator fun plus(other: IncompleteTickerConfig): IncompleteTickerConfig {
        return IncompleteTickerConfig(other.name ?: name,
            other.delay ?: delay,
            other.ref ?: ref,
            if (config is IncompleteBuildConfig) other.config.let { if (it is IncompleteBuildConfig) config + it else config }
            else other.config
        ).let { if (it.isEmpty()) throw InvalidParameterException(other.toString()) else it }
    }

    operator fun plus(other: TickerConfig): TickerConfig {
        val config = (this + IncompleteTickerConfig(other)).toConf()
        if (config is TickerConfig) return config
        else throw InvalidParameterException(other.toString())
    }

    fun toConf(): TickerConfig? {
        return if (name is String && delay is Int) {
            if (ref is String && ref != "") {
                if (config?.isEmpty() != false) TickerConfig.Ref(this.name, this.delay, this.ref)
                    .let { if (it.isInvalid()) null else it }
                else TickerConfig.Mix(this.name, this.delay, this.ref, this.config)
                    .let { if (it.isInvalid()) null else it }
            } else {
                if (this.config?.isEmpty() != false) null
                else this.config.toConf().let { conf ->
                    if (conf is BuildConfig && (!conf.isInvalid())) TickerConfig.Config(this.name, this.delay, conf)
                        .let { if (it.isInvalid()) null else it }
                    else null
                }
            }
        } else null
    }

    fun isEmpty(): Boolean {
        for (i in arrayOf(
            { this.name == null || this.name == "" },
            { this.delay == null || this.delay <= 0 },
            { (this.config == null || this.config.isEmpty()) && (this.ref == null || this.ref == "") },
        )) if (!i()) return false
        return true
    }
}
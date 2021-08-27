package com.github.frierenzk.ticker

import com.github.frierenzk.config.IncompleteBuildConfig
import com.github.frierenzk.config.IncompleteTickerConfig
import com.github.frierenzk.task.BuildConfig

sealed interface TickerConfig {
    val name: String
    val delay: Int

    fun isInvalid(): Boolean

    data class Ref constructor(
        override val name: String,
        override val delay: Int,
        val ref: String,
    ) : TickerConfig {
        override fun isInvalid(): Boolean {
            for (i in arrayOf(
                { this.name == "" },
                { this.delay <= 0 },
                { this.ref == "" }
            )) if (i()) return true
            return false
        }
    }

    data class Mix(
        override val name: String,
        override val delay: Int,
        val ref: String,
        val config: IncompleteBuildConfig,
    ) : TickerConfig {
        override fun isInvalid(): Boolean {
            for (i in arrayOf(
                { this.name == "" },
                { this.delay <= 0 },
                { this.ref == "" },
                { this.config.isEmpty() }
            )) if (i()) return true
            return false
        }
    }

    data class Config(
        override val name: String,
        override val delay: Int,
        val config: BuildConfig,
    ) : TickerConfig {
        override fun isInvalid(): Boolean {
            for (i in arrayOf(
                { this.name == "" },
                { this.delay <= 0 },
                { this.config.isInvalid() }
            )) if (i()) return true
            return false
        }
    }

    operator fun plus(other: IncompleteTickerConfig): TickerConfig {
        return other + this
    }

    operator fun plus(other: TickerConfig): TickerConfig {
        return this + IncompleteTickerConfig(other)
    }
}

package com.github.frierenzk.ticker

import com.github.frierenzk.config.ConfigOperator.projectGson
import com.github.frierenzk.config.IncompleteBuildConfig
import com.github.frierenzk.config.IncompleteTickerConfig
import com.github.frierenzk.task.BuildConfig
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TickerConfigTest {
    private val buildConfig by lazy { BuildConfig("testName", "testCategory", "test") }
    private val config1 by lazy {
        IncompleteTickerConfig("test",
            10,
            config = IncompleteBuildConfig(buildConfig)).toConf()
    }
    private val config2 by lazy { IncompleteTickerConfig("test", 10, ref = "test").toConf() }
    private val config3 by lazy {
        IncompleteTickerConfig("test",
            10,
            ref = "test",
            IncompleteBuildConfig(buildConfig)).toConf()
    }

    @Test
    fun convert() {
        assertTrue { config1 is TickerConfig.Config }
        assertTrue { config2 is TickerConfig.Ref }
        assertTrue { config3 is TickerConfig.Mix }
    }

    @Test
    fun parse() {
        projectGson.toJson(config1).let {
            println(it)
            assertTrue { it.contains("\"config\":") && !it.contains("\"ref\":") }
        }
        projectGson.toJson(config2).let {
            println(it)
            assertTrue { !it.contains("\"config\":") && it.contains("\"ref\":") }
        }
        projectGson.toJson(config3).let {
            println(it)
            assertTrue { it.contains("\"config\":") && it.contains("\"ref\":") }
        }
    }

    @Test
    fun load() {
        val conf1 = projectGson.fromJson(projectGson.toJson(config1), IncompleteTickerConfig::class.java).toConf()
        assertTrue { conf1 == config1 }

        val conf2 = projectGson.fromJson(projectGson.toJson(config2), IncompleteTickerConfig::class.java).toConf()
        assertTrue { conf2 == config2 }

        val conf3 = projectGson.fromJson(projectGson.toJson(config3), IncompleteTickerConfig::class.java).toConf()
        assertTrue { conf3 == config3 }
    }
}
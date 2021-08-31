package com.github.frierenzk.task

import com.github.frierenzk.config.ConfigOperator
import com.github.frierenzk.config.IncompleteBuildConfig
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class BuildConfigTest {
    private val buildConfig by lazy { BuildConfig("testName", "testCategory", "test") }
    private val buildConfig2 by lazy {
        buildConfig.copy(
            name = "testName2",
            category = "tags",
            extraParas = hashMapOf("source" to "\${default}/test")
        )
    }
    private val buildConfig3 by lazy {
        buildConfig.plus(
            BuildConfig(
                "testName3",
                "branches",
                "null",
                hashMapOf("svn" to "test", "upload" to "\${category}/\${name}/1234")
            )
        )
    }

    @Test
    @Order(1)
    fun getSource() {
        println(buildConfig.getSource())
        println(buildConfig2.getSource())
        println(buildConfig3.getSource())
    }

    @Test
    @Order(2)
    fun getUpload() {
        println(buildConfig.getUpload())
        println(buildConfig2.getUpload())
        println(buildConfig3.getUpload())
    }

    @Test
    @Order(3)
    fun equals() {
        val config = BuildConfig("testName2", "tags", "test", hashMapOf("source" to "\${default}/test"))
        assertTrue { config == buildConfig2 }
    }

    @Test
    @Order(4)
    fun parse() {
        val conf = buildConfig + IncompleteBuildConfig(profile = "1234")
        println(ConfigOperator.projectGson.toJson(conf))
        assertFalse { conf.isInvalid() }
        val conf2 = ConfigOperator.projectGson.fromJson(ConfigOperator.projectGson.toJson(conf),IncompleteBuildConfig::class.java).toConf()
        assertEquals(conf,conf2)
    }
}
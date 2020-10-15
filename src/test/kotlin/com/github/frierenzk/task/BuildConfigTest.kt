package com.github.frierenzk.task

import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class BuildConfigTest {
    private val buildConfig by lazy {
        BuildConfig().apply {
            this.name = "testName"
            this.category = "testCategory"
            this.profile = ""
        }
    }
    private val buildConfig2 by lazy {
        buildConfig.copy().apply {
            this.name = "testName2"
            this.category = "tags"
            this.sourcePath = "\$default/test"
        }
    }
    private val buildConfig3 by lazy {
        buildConfig.copy().apply {
            this.name = "testName3"
            this.projectDir = "testDir"
            this.category = "branches"
            this.svnBasePath = "\$default/.."
            this.uploadPath = "\$category/\$name/1234"
        }
    }

    @Test
    @Order(1)
    fun getFullSourcePath() {
        println(buildConfig.getFullSourcePath())
        println(buildConfig2.getFullSourcePath())
        println(buildConfig3.getFullSourcePath())
    }

    @Test
    @Order(2)
    fun getFullUploadPath() {
        println(buildConfig.getFullUploadPath())
        println(buildConfig2.getFullUploadPath())
        println(buildConfig3.getFullUploadPath())
    }

    @Test
    @Order(3)
    fun getFullSvnBasePath() {
        println(buildConfig.getFullSvnBasePath())
        println(buildConfig2.getFullSvnBasePath())
        println(buildConfig3.getFullSvnBasePath())
    }
}
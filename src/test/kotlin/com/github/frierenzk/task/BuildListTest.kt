package com.github.frierenzk.task

import org.junit.jupiter.api.Test
import java.io.File

internal class BuildListTest {
    companion object {
        @JvmStatic
        val buildList = BuildList()
    }

    @Test
    fun generate() {
        buildList.generate(File("build_list.json"))
    }
}
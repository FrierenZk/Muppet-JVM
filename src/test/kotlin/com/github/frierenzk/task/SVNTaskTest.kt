package com.github.frierenzk.task

import org.junit.jupiter.api.*
import java.io.File
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SVNTaskTest {
    private val uri: URI by lazy { File("build").toURI() }

    @Test
    @Order(1)
    internal fun infoTest() {
        val task = SVNTask.buildSVNTask(uri)
        println(task.info().isBlank())
        task.outBufferedReader.forEachLine {
            println(it)
        }
        task.errorBufferedReader.forEachLine {
            println(it)
        }
    }

    @Test
    @Order(2)
    internal fun updateTest() {
        val task = SVNTask.buildSVNTask(uri)
        task.update()
        task.outBufferedReader.forEachLine {
            println(it)
        }
        task.errorBufferedReader.forEachLine {
            println(it)
        }
    }

    @Test
    @Order(3)
    internal fun checkOutTest() {
        val svnPath = "svn://noway"
        println("path is ${SVNTask.isSVNPath(svnPath)}")
        val task = SVNTask.buildSVNCheckOutTask(File("build/virtual").toURI(), svnPath)
        println(task.info().isBlank())
        task.checkOut()
        task.outBufferedReader.forEachLine {
            println(it)
        }
        task.errorBufferedReader.forEachLine {
            println(it)
        }
    }
}
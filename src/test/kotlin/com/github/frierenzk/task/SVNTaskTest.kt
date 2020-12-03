package com.github.frierenzk.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SVNTaskTest {
    private val uri: URI by lazy { File("build/tmp/subversion").toURI() }

    @Test
    @Order(1)
    internal fun checkOutTest() {
        try {
            File(uri).deleteRecursively()
        } catch (exception: Exception) {
            exception.printStackTrace()
            println(exception.message)
        }
        val svnPath = "https://svn.apache.org/repos/asf/subversion/trunk/doc/programmer/"
        assertEquals(SVNTask.isURL(svnPath), true)
        val task = SVNTask.buildSVNCheckOutTask(uri, svnPath)
        assertEquals(task.info().isBlank(), false)
        task.outBufferedReader.forEachLine {
            println(it)
        }
        task.errorBufferedReader.forEachLine {
            println(it)
        }
        task.checkOut()
        task.outBufferedReader.forEachLine {
            println(it)
        }
        task.errorBufferedReader.forEachLine {
            println(it)
        }
    }

    @Test
    @Order(2)
    internal fun infoTest() {
        val task = SVNTask.buildSVNTask(uri)
        assertEquals(task.info().isBlank(), false)
        task.outBufferedReader.forEachLine {
            println(it)
        }
        task.errorBufferedReader.forEachLine {
            println(it)
        }
    }

    @Test
    @Order(3)
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
}
package com.github.frierenzk.dispatcher

import com.github.frierenzk.utils.UnstableApi
import kotlin.reflect.full.isSuperclassOf

open class Pipe<T1, T2>(val data: T1, val callback: (T2) -> Unit) {
    companion object {
        val default by lazy { Pipe<Unit, Unit>(Unit) {} }

        fun <T> data(data: T): Pipe<T, Unit> {
            return Pipe(data) {}
        }

        fun <T> callback(callback: (T) -> Unit): Pipe<Unit, T> {
            return Pipe(Unit, callback)
        }
    }

    inline fun <reified A, reified B> asPipe(): Pipe<A, B>? {
        return if (isPipe<A, B>())
            @Suppress("UNCHECKED_CAST")
            this as Pipe<A, B>
        else null
    }

    @UnstableApi
    inline fun <reified A, reified B> isPipe(): Boolean {
        //Can not use "callback is (B)->Unit" to check types
        return data is A &&
                callback::class.java.methods.filter { it.name == "invoke" }
                    .map { B::class.isSuperclassOf(it.parameters.first().type.kotlin) }.contains(true)
    }

    inline fun <reified T> isCallbackPipe() = isPipe<Unit, T>()
    inline fun <reified T> asCallbackPipe() = asPipe<Unit, T>()

    inline fun <reified T> isDataPipe() = isPipe<T, Unit>()
    inline fun <reified T> asDataPipe() = asPipe<T, Unit>()

    fun runIf(condition: Boolean, runnable: () -> Unit) {
        if (condition) return runnable()
    }
}
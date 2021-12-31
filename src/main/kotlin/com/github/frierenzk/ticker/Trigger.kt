package com.github.frierenzk.ticker

import java.util.*

class Trigger(val tickerConfig: TickerConfig) {
    var launch: (() -> Unit)? = null
    private var snapshot = Calendar.getInstance().time

    fun knock(force: Boolean = false) {
        val calendar = Calendar.getInstance()
        calendar.time = snapshot
        calendar.add(Calendar.MINUTE, tickerConfig.delay)
        val now = Calendar.getInstance().time
        if (calendar.time.before(now) || force) try {
            launch?.invoke()
        } catch (exception: Exception) {
            exception.printStackTrace()
        } finally {
            snapshot = now
        }
    }
}
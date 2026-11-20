package com.trazo.app.data.clock

fun interface MonotonicClock {
    fun nowMillis(): Long
}

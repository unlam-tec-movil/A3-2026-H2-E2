package com.trazo.app.domain.repository

fun interface MonotonicClock {
    fun nowMillis(): Long
}

package com.trazo.app.domain.model.map

import kotlin.time.Instant

data class InitializedRunSession(
    val id: String,
    val goal: RunGoal,
    val startMonotonicTimestamp: Long,
    val startedAt: Instant,
    val isDemoMode: Boolean = false,
)

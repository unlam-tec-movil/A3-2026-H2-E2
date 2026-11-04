package com.trazo.app.domain.model.map

data class InitializedRunSession(
    val id: String,
    val goal: RunGoal,
    val startMonotonicTimestamp: Long,
)

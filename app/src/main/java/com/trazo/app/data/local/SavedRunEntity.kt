package com.trazo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_runs")
data class SavedRunEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startedAtMillis: Long,
    val goalType: String,
    val goalReached: Boolean,
    val isDemo: Boolean,
    val distanceMeters: Double,
    val elapsedTimeSeconds: Long,
    val averageSpeedKmh: Double,
    val regularityPercent: Int?,
    val routePointsJson: String,
    val brakesJson: String,
    val highlightsJson: String,
    val photoPathsJson: String,
)

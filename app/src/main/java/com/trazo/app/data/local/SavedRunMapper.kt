package com.trazo.app.data.local

import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import kotlin.time.Instant

internal fun SavedRun.toEntity(): SavedRunEntity =
    SavedRunEntity(
        id = id,
        title = title,
        startedAtMillis = startedAt.toEpochMilliseconds(),
        goalType = goal.name,
        goalReached = goalReached,
        isDemo = isDemo,
        distanceMeters = distanceMeters,
        elapsedTimeSeconds = elapsedTimeSeconds,
        averageSpeedKmh = averageSpeedKmh,
        regularityPercent = regularityPercent,
        routePointsJson = routePoints.routeToJson(),
        brakesJson = brakes.brakesToJson(),
        highlightsJson = highlights.textsToJson(),
        photoPathsJson = photoPaths.textsToJson(),
    )

internal fun SavedRunEntity.toDomain(): SavedRun =
    SavedRun(
        id = id,
        title = title,
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        goal = RunGoal.valueOf(goalType),
        goalReached = goalReached,
        isDemo = isDemo,
        distanceMeters = distanceMeters,
        elapsedTimeSeconds = elapsedTimeSeconds,
        averageSpeedKmh = averageSpeedKmh,
        routePoints = routePointsJson.jsonToRoute(),
        brakes = brakesJson.jsonToBrakes(),
        regularityPercent = regularityPercent,
        highlights = highlightsJson.jsonToTexts(),
        photoPaths = photoPathsJson.jsonToTexts(),
    )

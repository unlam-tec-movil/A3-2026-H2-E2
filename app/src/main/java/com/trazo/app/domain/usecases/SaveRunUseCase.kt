package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.saved.SavedRun
import javax.inject.Inject

/** Resultado de intentar guardar un recorrido. */
sealed interface SaveRunResult {
    data object Saved : SaveRunResult

    data object InvalidTitle : SaveRunResult

    data object TooManyPhotos : SaveRunResult

    data object RunNotFound : SaveRunResult
}

/** Valida lo que cargó el usuario, arma el [SavedRun] con lo medido y lo guarda. */
class SaveRunUseCase
    @Inject
    constructor(
        private val runSessionRepository: RunSessionRepository,
        private val savedRunRepository: SavedRunRepository,
    ) {
        suspend operator fun invoke(
            runId: String,
            title: String,
            highlights: List<String>,
            photoPaths: List<String>,
        ): SaveRunResult {
            val cleanTitle = title.trim()
            if (cleanTitle.length !in TITLE_LENGTH) return SaveRunResult.InvalidTitle
            if (photoPaths.size > SavedRun.MAX_PHOTOS) return SaveRunResult.TooManyPhotos
            val summary =
                runSessionRepository.getFinishedSummary(runId) ?: return SaveRunResult.RunNotFound

            savedRunRepository.save(
                SavedRun(
                    id = summary.id,
                    title = cleanTitle,
                    startedAt = summary.startedAt,
                    goal = summary.goal,
                    goalReached = summary.goalReached,
                    isDemo = summary.isDemoMode,
                    distanceMeters = summary.metrics.distanceMeters,
                    elapsedTimeSeconds = summary.metrics.elapsedTimeSeconds,
                    averageSpeedKmh = summary.metrics.averageSpeedKmh,
                    routePoints = summary.routePoints,
                    brakes = summary.brakes,
                    regularityPercent = summary.regularityPercent,
                    highlights = highlights,
                    photoPaths = photoPaths,
                ),
            )
            return SaveRunResult.Saved
        }

        private companion object {
            val TITLE_LENGTH = 3..50
        }
    }

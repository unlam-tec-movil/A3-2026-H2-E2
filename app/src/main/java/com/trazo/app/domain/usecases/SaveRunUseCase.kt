package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.saved.SavedRun
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Resultado de intentar guardar un recorrido. */
sealed interface SaveRunResult {
    data object Saved : SaveRunResult

    data object InvalidTitle : SaveRunResult

    data object TooManyPhotos : SaveRunResult

    data object RunNotFound : SaveRunResult

    /** El título y el recorrido eran válidos, pero no se pudo escribir en el almacenamiento. */
    data object SaveFailed : SaveRunResult
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

            val savedRun =
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
                    // SavedRun todavía no distingue «sin dato» (null, sin acelerómetro) de 0 frenadas.
                    brakes = summary.brakes.orEmpty(),
                    regularityPercent = summary.regularityPercent,
                    highlights = highlights,
                    photoPaths = photoPaths,
                )
            return try {
                savedRunRepository.save(savedRun)
                SaveRunResult.Saved
            } catch (error: CancellationException) {
                // Si se cancela la corrutina, hay que dejarla cancelarse: no es un fallo de guardado.
                throw error
            } catch (error: Exception) {
                SaveRunResult.SaveFailed
            }
        }

        private companion object {
            val TITLE_LENGTH = SavedRun.TITLE_MIN_LENGTH..SavedRun.TITLE_MAX_LENGTH
        }
    }

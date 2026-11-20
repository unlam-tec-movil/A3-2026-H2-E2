package com.trazo.app.domain.model.map

import kotlin.time.Instant

sealed interface RunSession {
    data object Idle : RunSession

    /**
     * Recorrido en curso con sus reglas: cada transición recibe el instante del reloj monotónico y
     * devuelve una copia nueva, o lanza [IllegalStateException] si no se puede desde el estado actual.
     */
    data class ActiveSession(
        val id: String,
        val goal: RunGoal,
        val state: RunState,
        val routePoints: List<RoutePoint> = emptyList(),
        val metrics: RunMetrics = RunMetrics(),
        val startMonotonicTimestamp: Long,
        val startedAt: Instant,
        val isDemoMode: Boolean = false,
        val lastValidLocationTimestamp: Long? = null,
        val accumulatedElapsedTimeMillis: Long = 0L,
        val resumedAtMonotonicTimestamp: Long? = null,
        val gpsQuality: GpsQuality = GpsQuality.NO_SIGNAL,
        val goalReached: Boolean = false,
    ) : RunSession {
        /** Tiempo en movimiento acumulado: los tramos cerrados más el tramo abierto, si lo hay. */
        fun elapsedAt(nowMillis: Long): Long {
            val currentSegment = resumedAtMonotonicTimestamp?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L
            return accumulatedElapsedTimeMillis + currentSegment
        }

        /**
         * Si el recorrido sigue registrando: en [RunState.ACTIVE] y en [RunState.STOPPED] (una frenada
         * es parte del recorrido, stop-detection D3) corren el temporizador y el GPS.
         */
        val isTracking: Boolean
            get() = state == RunState.ACTIVE || state == RunState.STOPPED

        /** Pausa manual del corredor, también en medio de una frenada. */
        fun pause(nowMillis: Long): ActiveSession {
            check(isTracking) { "El recorrido no está en curso." }
            return frozenAt(nowMillis, RunState.PAUSED)
        }

        /** Pausa personal, después de contestar la consulta de detención (stop-detection D1). */
        fun pausePersonally(nowMillis: Long): ActiveSession {
            check(isTracking) { "El recorrido no está en curso." }
            return frozenAt(nowMillis, RunState.PERSONAL_PAUSED)
        }

        /** Detención automática (frenada): el tramo sigue abierto y el tiempo cuenta (stop-detection D3). */
        fun markStopped(nowMillis: Long): ActiveSession {
            check(state == RunState.ACTIVE) { "El recorrido no está activo." }
            return copy(state = RunState.STOPPED, metrics = metricsAt(nowMillis))
        }

        /**
         * Vuelve a moverse después de una frenada o de una pausa personal (stop-detection D2). Desde la
         * frenada el tramo ya estaba abierto; desde la pausa personal se abre uno nuevo.
         */
        fun markMoving(nowMillis: Long): ActiveSession =
            when (state) {
                RunState.STOPPED -> copy(state = RunState.ACTIVE, metrics = metricsAt(nowMillis))
                RunState.PERSONAL_PAUSED -> activeFrom(nowMillis)
                else -> throw IllegalStateException("La sesión no está detenida ni en pausa personal.")
            }

        /** Reanuda una pausa manual o personal. */
        fun resume(nowMillis: Long): ActiveSession {
            check(state == RunState.PAUSED || state == RunState.PERSONAL_PAUSED) {
                "El recorrido no está pausado manualmente."
            }
            return activeFrom(nowMillis)
        }

        /** Suma un punto preciso a la traza y recalcula las métricas. Si no [isTracking], no cambia nada. */
        fun addPoint(
            point: RoutePoint,
            nowMillis: Long,
        ): ActiveSession {
            if (!isTracking) return this

            val additionalDistance = routePoints.lastOrNull()?.distanceMetersTo(point) ?: 0.0
            val totalDistance = metrics.distanceMeters + additionalDistance
            val elapsedTime = elapsedAt(nowMillis)

            return copy(
                routePoints = routePoints + point,
                metrics =
                    RunMetrics(
                        elapsedTimeSeconds = elapsedTime / 1_000L,
                        distanceMeters = totalDistance,
                        currentSpeedKmh = point.speedMetersPerSecond.coerceAtLeast(0f) * 3.6,
                        averageSpeedKmh = averageSpeedKmh(totalDistance, elapsedTime),
                    ),
                lastValidLocationTimestamp = point.monotonicTimestampMillis,
                goalReached = goalReached || totalDistance >= goal.distanceKm * 1_000.0,
            )
        }

        /** Instante desde el que se mide la pérdida de señal: último punto válido o última reanudación. */
        fun signalReferenceAt(nowMillis: Long): Long = lastValidLocationTimestamp ?: resumedAtMonotonicTimestamp ?: nowMillis

        /** Si en [nowMillis] la señal ya se considera perdida, según [GpsSignalRules]. */
        fun isSignalLostAt(nowMillis: Long): Boolean = GpsSignalRules.isSignalLost(signalReferenceAt(nowMillis), nowMillis)

        /** Avance del reloj: actualiza tiempo y velocidad promedio, y marca sin señal si se perdió. */
        fun tick(
            nowMillis: Long,
            signalLost: Boolean,
        ): ActiveSession {
            if (!isTracking) return this

            return copy(
                gpsQuality = if (signalLost) GpsQuality.NO_SIGNAL else gpsQuality,
                metrics = metricsAt(nowMillis),
            )
        }

        /** Calidad de señal informada por el GPS. Si no [isTracking], no cambia nada. */
        fun withGpsQuality(quality: GpsQuality): ActiveSession = if (isTracking) copy(gpsQuality = quality) else this

        /** Resumen final del recorrido, con las métricas cerradas en [nowMillis]. */
        fun toFrozenSummary(nowMillis: Long): FrozenRunSummary =
            FrozenRunSummary(
                id = id,
                goal = goal,
                routePoints = routePoints.toList(),
                metrics = metricsAt(nowMillis),
                startedAt = startedAt,
                startedAtMonotonicTimestamp = startMonotonicTimestamp,
                finishedAtMonotonicTimestamp = nowMillis,
                goalReached = goalReached,
                isDemoMode = isDemoMode,
            )

        /** Métricas con el tiempo y la velocidad promedio de [nowMillis]; la distancia no cambia. */
        private fun metricsAt(nowMillis: Long): RunMetrics {
            val elapsedTime = elapsedAt(nowMillis)
            return metrics.copy(
                elapsedTimeSeconds = elapsedTime / 1_000L,
                averageSpeedKmh = averageSpeedKmh(metrics.distanceMeters, elapsedTime),
            )
        }

        /** Cierra el tramo abierto: congela tiempo y velocidad promedio y pasa a [newState]. */
        private fun frozenAt(
            nowMillis: Long,
            newState: RunState,
        ): ActiveSession =
            copy(
                state = newState,
                metrics = metricsAt(nowMillis),
                accumulatedElapsedTimeMillis = elapsedAt(nowMillis),
                resumedAtMonotonicTimestamp = null,
            )

        /** Abre un tramo nuevo en [nowMillis]; la señal arranca débil hasta el próximo punto. */
        private fun activeFrom(nowMillis: Long): ActiveSession =
            copy(
                state = RunState.ACTIVE,
                lastValidLocationTimestamp = null,
                resumedAtMonotonicTimestamp = nowMillis,
                gpsQuality = GpsQuality.WEAK_SIGNAL,
            )

        companion object {
            /** Sesión recién iniciada: activa, sin puntos y con el primer tramo abierto en [nowMillis]. */
            fun start(
                session: InitializedRunSession,
                nowMillis: Long,
            ): ActiveSession =
                ActiveSession(
                    id = session.id,
                    goal = session.goal,
                    state = RunState.ACTIVE,
                    startMonotonicTimestamp = session.startMonotonicTimestamp,
                    startedAt = session.startedAt,
                    isDemoMode = session.isDemoMode,
                    resumedAtMonotonicTimestamp = nowMillis,
                    gpsQuality = GpsQuality.WEAK_SIGNAL,
                )

            /** Velocidad promedio en km/h; 0 si todavía no pasó tiempo. */
            fun averageSpeedKmh(
                distanceMeters: Double,
                elapsedMillis: Long,
            ): Double {
                if (elapsedMillis <= 0L) return 0.0
                return (distanceMeters / 1_000.0) / (elapsedMillis / 3_600_000.0)
            }
        }
    }
}

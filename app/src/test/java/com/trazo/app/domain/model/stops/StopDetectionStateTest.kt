package com.trazo.app.domain.model.stops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopDetectionStateTest {
    private val brake = BrakeEvent(id = "b1", runId = "r1", location = null, detectedAtMillis = 0L)

    @Test
    fun `el estado inicial es movimiento sin consulta pendiente`() {
        val state = StopDetectionState()

        assertEquals(StopPhase.Moving, state.phase)
        assertNull(state.pendingClassificationBrakeId)
    }

    @Test
    fun `Still y PersonalPause no tienen consulta pendiente`() {
        assertNull(StopDetectionState(StopPhase.Still(sinceMillis = 1_000L)).pendingClassificationBrakeId)
        assertNull(StopDetectionState(StopPhase.PersonalPause(sinceMillis = 1_000L)).pendingClassificationBrakeId)
    }

    @Test
    fun `la consulta pendiente solo existe con query PENDING`() {
        fun pendingId(query: QueryStatus) =
            StopDetectionState(
                phase = StopPhase.Braking(sinceMillis = 0L, brake = brake, query = query),
            ).pendingClassificationBrakeId

        assertNull(pendingId(QueryStatus.NOT_DUE))
        assertEquals("b1", pendingId(QueryStatus.PENDING))
        assertNull(pendingId(QueryStatus.ANSWERED))
    }

    @Test
    fun `la configuracion trae los valores por defecto del diseno`() {
        val config = StopDetectionConfig()

        assertEquals(8_000L, config.brakeAfterMillis)
        assertEquals(20_000L, config.classifyAfterMillis)
        assertEquals(1_500L, config.resumeConfirmMillis)
        assertEquals(500L, config.tickIntervalMillis)
        assertEquals(1_000L, config.windowMillis)
        assertEquals(2_000L, config.sampleStaleMillis)
    }
}

package com.trazo.app.domain.model.stops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopDetectionStateTest {
    @Test
    fun `el estado inicial es movimiento continuo sin consulta pendiente`() {
        val state = StopDetectionState()

        assertEquals(MovementState.CONTINUOUS_MOVEMENT, state.movement)
        assertNull(state.pendingClassificationBrakeId)
    }

    @Test
    fun `Still se expone como movimiento continuo`() {
        val state = StopDetectionState(phase = StopPhase.Still(sinceMillis = 1_000L))

        assertEquals(MovementState.CONTINUOUS_MOVEMENT, state.movement)
        assertNull(state.pendingClassificationBrakeId)
    }

    @Test
    fun `PersonalPause se expone como movimiento continuo`() {
        val state = StopDetectionState(phase = StopPhase.PersonalPause(sinceMillis = 1_000L))

        assertEquals(MovementState.CONTINUOUS_MOVEMENT, state.movement)
        assertNull(state.pendingClassificationBrakeId)
    }

    @Test
    fun `Braking se expone como BRAKING`() {
        QueryStatus.entries.forEach { query ->
            val state =
                StopDetectionState(
                    phase = StopPhase.Braking(sinceMillis = 0L, brakeId = "b1", query = query),
                )

            assertEquals(MovementState.BRAKING, state.movement)
        }
    }

    @Test
    fun `la consulta pendiente solo existe con query PENDING`() {
        fun pendingId(query: QueryStatus) =
            StopDetectionState(
                phase = StopPhase.Braking(sinceMillis = 0L, brakeId = "b1", query = query),
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

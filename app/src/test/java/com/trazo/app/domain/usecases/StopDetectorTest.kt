package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.MovementState
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.model.stops.StopPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StopDetectorTest {
    private var nextId = 0
    private val detector = StopDetector(StopDetectionConfig()) { "b${nextId++}" }
    private val location = StopLocation(-34.6, -58.4, 5f)

    private fun obs(
        now: Long,
        still: Boolean,
        loc: StopLocation? = location,
    ) = Observation(now, still, loc)

    @Test
    fun `el estado inicial es movimiento sin frenadas`() {
        val state = detector.initial()

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(state.brakes.isEmpty())
    }

    @Test
    fun `en movimiento continuo no se registra nada`() {
        var state = detector.initial()
        for (t in 0..10_000L step 500) state = detector.advance(state, obs(t, still = false))

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(state.brakes.isEmpty())
    }

    @Test
    fun `quieto 7999 ms y retoma no registra frenada`() {
        var state = detector.advance(detector.initial(), obs(0, still = true))
        state = detector.advance(state, obs(7_999, still = true))
        state = detector.advance(state, obs(7_999, still = false))

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(state.brakes.isEmpty())
    }

    @Test
    fun `quieto 8000 ms registra una frenada con ubicacion`() {
        var state = detector.advance(detector.initial(), obs(1_000, still = true))
        state = detector.advance(state, obs(9_000, still = true))

        assertEquals(MovementState.BRAKING, state.movement)
        assertEquals(1, state.brakes.size)
        assertEquals(location, state.brakes[0].location)
        assertEquals(9_000L, state.brakes[0].detectedAtMillis)
        assertEquals(1_000L, (state.phase as StopPhase.Braking).sinceMillis)
    }

    @Test
    fun `sin ubicacion la frenada se registra con location nula`() {
        var state = detector.advance(detector.initial(), obs(0, still = true, loc = null))
        state = detector.advance(state, obs(8_000, still = true, loc = null))

        assertEquals(1, state.brakes.size)
        assertNull(state.brakes[0].location)
    }

    @Test
    fun `30 observaciones seguidas quietas registran una sola frenada`() {
        var state: StopDetectionState = detector.initial()
        for (i in 0 until 30) state = detector.advance(state, obs(i * 500L, still = true))

        assertEquals(1, state.brakes.size)
    }

    @Test
    fun `onRunStopped vuelve a Moving y conserva las frenadas`() {
        var state = detector.advance(detector.initial(), obs(0, still = true))
        state = detector.advance(state, obs(8_000, still = true))

        val stopped = detector.onRunStopped(state)

        assertEquals(StopPhase.Moving, stopped.phase)
        assertEquals(state.brakes, stopped.brakes)
    }
}

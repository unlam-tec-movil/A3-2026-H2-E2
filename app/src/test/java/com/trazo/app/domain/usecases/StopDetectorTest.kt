package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.MovementState
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.model.stops.StopPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        state = detector.advance(state, obs(9_499, still = false))

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(state.brakes.isEmpty())
    }

    private fun brakingAt(
        since: Long,
        at: Long = since + 8_000,
    ): StopDetectionState {
        var state = detector.advance(detector.initial(), obs(since, still = true))
        state = detector.advance(state, obs(at, still = true))
        return state
    }

    @Test
    fun `un pico de 500 ms durante la quietud no cancela la cuenta de 8 s`() {
        var state = detector.advance(detector.initial(), obs(0, still = true))
        state = detector.advance(state, obs(5_000, still = false))
        state = detector.advance(state, obs(5_500, still = true))
        state = detector.advance(state, obs(8_000, still = true))

        assertEquals(MovementState.BRAKING, state.movement)
        assertEquals(1, state.brakes.size)
    }

    @Test
    fun `picos de 500 ms en frenada no resetean ni duplican la frenada`() {
        var state = brakingAt(0)
        state = detector.advance(state, obs(8_500, still = false))
        state = detector.advance(state, obs(9_000, still = true))
        state = detector.advance(state, obs(10_000, still = false))
        state = detector.advance(state, obs(10_500, still = true))
        state = detector.advance(state, obs(12_000, still = true))

        assertEquals(MovementState.BRAKING, state.movement)
        assertEquals(1, state.brakes.size)
        assertNull((state.phase as StopPhase.Braking).movingSinceMillis)
    }

    @Test
    fun `movimiento de 1500 ms tras la frenada vuelve a Moving y conserva la frenada`() {
        var state = brakingAt(0)
        state = detector.advance(state, obs(9_000, still = false))
        state = detector.advance(state, obs(10_499, still = false))
        assertEquals(MovementState.BRAKING, state.movement)

        state = detector.advance(state, obs(10_500, still = false))

        assertEquals(StopPhase.Moving, state.phase)
        assertEquals(1, state.brakes.size)
    }

    @Test
    fun `tras retomar y volver a quedarse quieto se registra una segunda frenada`() {
        var state = brakingAt(0)
        state = detector.advance(state, obs(9_000, still = false))
        state = detector.advance(state, obs(10_500, still = false))
        state = detector.advance(state, obs(11_000, still = true))
        state = detector.advance(state, obs(19_000, still = true))

        assertEquals(2, state.brakes.size)
    }

    @Test
    fun `20000 ms desde el inicio de la quietud no consulta y 20001 ms si`() {
        var state = brakingAt(0)
        state = detector.advance(state, obs(20_000, still = true))
        assertNull(state.pendingClassificationBrakeId)

        state = detector.advance(state, obs(20_001, still = true))

        assertEquals(state.brakes[0].id, state.pendingClassificationBrakeId)
    }

    @Test
    fun `clasificar como frenada mantiene el registro y cierra la consulta`() {
        var state = brakingAt(0, at = 8_000)
        state = detector.advance(state, obs(20_001, still = true))

        state = detector.classify(state, StopKind.BRAKE)

        assertNull(state.pendingClassificationBrakeId)
        assertEquals(QueryStatus.ANSWERED, (state.phase as StopPhase.Braking).query)
        assertEquals(1, state.brakes.size)
        assertEquals(location, state.brakes[0].location)
        state = detector.advance(state, obs(25_000, still = true))
        assertEquals(QueryStatus.ANSWERED, (state.phase as StopPhase.Braking).query)
    }

    @Test
    fun `clasificar como pausa personal quita la frenada y no la registra de nuevo`() {
        var state = brakingAt(0, at = 8_000)
        state = detector.advance(state, obs(20_001, still = true))

        state = detector.classify(state, StopKind.PERSONAL_PAUSE)

        assertTrue(state.brakes.isEmpty())
        assertEquals(MovementState.CONTINUOUS_MOVEMENT, state.movement)
        assertTrue(state.phase is StopPhase.PersonalPause)
        for (t in 21_000..60_000L step 500) state = detector.advance(state, obs(t, still = true))
        assertTrue(state.brakes.isEmpty())
        assertTrue(state.phase is StopPhase.PersonalPause)
    }

    @Test
    fun `desde la pausa personal retomar 1500 ms vuelve a Moving y un pico no`() {
        var state = brakingAt(0, at = 8_000)
        state = detector.advance(state, obs(20_001, still = true))
        state = detector.classify(state, StopKind.PERSONAL_PAUSE)

        state = detector.advance(state, obs(21_000, still = false))
        state = detector.advance(state, obs(21_500, still = true))
        assertTrue(state.phase is StopPhase.PersonalPause)

        state = detector.advance(state, obs(22_000, still = false))
        state = detector.advance(state, obs(23_500, still = false))

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(state.brakes.isEmpty())
    }

    @Test
    fun `retomar sin responder cierra la consulta y mantiene la frenada`() {
        var state = brakingAt(0, at = 8_000)
        state = detector.advance(state, obs(20_001, still = true))
        assertNotNull(state.pendingClassificationBrakeId)

        state = detector.advance(state, obs(21_000, still = false))
        state = detector.advance(state, obs(22_500, still = false))

        assertNull(state.pendingClassificationBrakeId)
        assertEquals(StopPhase.Moving, state.phase)
        assertEquals(1, state.brakes.size)
    }

    @Test
    fun `clasificar sin consulta pendiente no cambia el estado`() {
        val moving = detector.initial()
        val still = detector.advance(moving, obs(0, still = true))
        val braking = brakingAt(0)

        assertEquals(moving, detector.classify(moving, StopKind.PERSONAL_PAUSE))
        assertEquals(still, detector.classify(still, StopKind.BRAKE))
        assertEquals(braking, detector.classify(braking, StopKind.PERSONAL_PAUSE))
        assertEquals(braking, detector.classify(braking, StopKind.BRAKE))
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

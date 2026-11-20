package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.ClosedStop
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

    /** Paradas cerradas en los pasos aplicados con [step]. */
    private val closed = mutableListOf<ClosedStop>()

    private fun obs(
        now: Long,
        still: Boolean,
        loc: StopLocation? = location,
    ) = Observation("r1", now, still, loc)

    private fun StopDetectionState.step(
        now: Long,
        still: Boolean,
        loc: StopLocation? = location,
    ): StopDetectionState = keep(detector.advance(this, obs(now, still, loc)))

    private fun StopDetectionState.answer(kind: StopKind): StopDetectionState = keep(detector.classify(this, kind))

    private fun keep(step: StopStep): StopDetectionState {
        step.closed?.let { closed += it }
        return step.state
    }

    private fun brakingAt(
        since: Long,
        at: Long = since + 8_000,
    ): StopDetectionState = detector.initial().step(since, still = true).step(at, still = true)

    private val StopDetectionState.braking get() = phase as StopPhase.Braking

    @Test
    fun `el estado inicial es movimiento`() {
        assertEquals(StopPhase.Moving, detector.initial().phase)
    }

    @Test
    fun `en movimiento continuo no pasa nada`() {
        var state = detector.initial()
        for (t in 0..10_000L step 500) state = state.step(t, still = false)

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(closed.isEmpty())
    }

    @Test
    fun `quieto 7999 ms y retoma no inicia una frenada`() {
        val state =
            detector
                .initial()
                .step(0, still = true)
                .step(7_999, still = true)
                .step(7_999, still = false)
                .step(9_499, still = false)

        assertEquals(StopPhase.Moving, state.phase)
        assertTrue(closed.isEmpty())
    }

    @Test
    fun `quieto 8000 ms inicia una frenada con ubicacion y recorrido`() {
        val state = detector.initial().step(1_000, still = true).step(9_000, still = true)

        val brake = state.braking.brake
        assertEquals(location, brake.location)
        assertEquals("r1", brake.runId)
        assertEquals(9_000L, brake.detectedAtMillis)
        assertEquals(1_000L, state.braking.sinceMillis)
        assertTrue("la frenada recién se cierra al terminar la parada", closed.isEmpty())
    }

    @Test
    fun `sin ubicacion la frenada tiene location nula`() {
        val state = detector.initial().step(0, still = true, loc = null).step(8_000, still = true, loc = null)

        assertNull(state.braking.brake.location)
    }

    @Test
    fun `un pico de 500 ms durante la quietud no cancela la cuenta de 8 s`() {
        val state =
            detector
                .initial()
                .step(0, still = true)
                .step(5_000, still = false)
                .step(5_500, still = true)
                .step(8_000, still = true)

        assertTrue(state.phase is StopPhase.Braking)
    }

    @Test
    fun `picos de 500 ms en frenada no cambian la frenada ni la cierran`() {
        val start = brakingAt(0)
        val state =
            start
                .step(8_500, still = false)
                .step(9_000, still = true)
                .step(10_000, still = false)
                .step(10_500, still = true)
                .step(12_000, still = true)

        assertEquals(start.braking.brake, state.braking.brake)
        assertNull(state.braking.movingSinceMillis)
        assertTrue(closed.isEmpty())
    }

    @Test
    fun `30 observaciones seguidas quietas generan una sola frenada`() {
        var state: StopDetectionState = detector.initial()
        for (i in 0 until 30) state = state.step(i * 500L, still = true)

        assertEquals(1, nextId)
    }

    @Test
    fun `retomar 1500 ms tras la frenada vuelve a Moving y la cierra sin respuesta`() {
        val start = brakingAt(0)
        var state = start.step(9_000, still = false).step(10_499, still = false)
        assertTrue(state.phase is StopPhase.Braking)

        state = state.step(10_500, still = false)

        assertEquals(StopPhase.Moving, state.phase)
        assertEquals(listOf(ClosedStop(start.braking.brake, answer = null)), closed)
    }

    @Test
    fun `tras retomar y volver a quedarse quieto empieza una segunda frenada`() {
        val state =
            brakingAt(0)
                .step(9_000, still = false)
                .step(10_500, still = false)
                .step(11_000, still = true)
                .step(19_000, still = true)

        assertEquals("b1", state.braking.brake.id)
        assertEquals(listOf("b0"), closed.map { it.brake.id })
    }

    @Test
    fun `20000 ms desde el inicio de la quietud no consulta y 20001 ms si`() {
        var state = brakingAt(0).step(20_000, still = true)
        assertNull(state.pendingClassificationBrakeId)

        state = state.step(20_001, still = true)

        assertEquals("b0", state.pendingClassificationBrakeId)
    }

    @Test
    fun `contestar frenada cierra la parada como frenada una sola vez`() {
        var state = brakingAt(0).step(20_001, still = true).answer(StopKind.BRAKE)

        assertNull(state.pendingClassificationBrakeId)
        assertEquals(QueryStatus.ANSWERED, state.braking.query)
        assertEquals(StopKind.BRAKE, closed.single().answer)
        assertEquals(location, closed.single().brake.location)

        state = state.step(25_000, still = true).step(26_000, still = false).step(27_500, still = false)

        assertEquals(StopPhase.Moving, state.phase)
        assertEquals("retomar no la vuelve a cerrar", 1, closed.size)
    }

    @Test
    fun `contestar pausa personal la cierra como pausa y no inicia otra frenada`() {
        var state = brakingAt(0).step(20_001, still = true).answer(StopKind.PERSONAL_PAUSE)

        assertTrue(state.phase is StopPhase.PersonalPause)
        assertEquals(StopKind.PERSONAL_PAUSE, closed.single().answer)

        for (t in 21_000..60_000L step 500) state = state.step(t, still = true)

        assertTrue(state.phase is StopPhase.PersonalPause)
        assertEquals(1, closed.size)
        assertEquals(1, nextId)
    }

    @Test
    fun `desde la pausa personal retomar 1500 ms vuelve a Moving y un pico no`() {
        var state =
            brakingAt(0)
                .step(20_001, still = true)
                .answer(StopKind.PERSONAL_PAUSE)
                .step(21_000, still = false)
                .step(21_500, still = true)
        assertTrue(state.phase is StopPhase.PersonalPause)

        state = state.step(22_000, still = false).step(23_500, still = false)

        assertEquals(StopPhase.Moving, state.phase)
        assertEquals(1, closed.size)
    }

    @Test
    fun `retomar sin responder cierra la consulta y la parada sin respuesta`() {
        var state = brakingAt(0).step(20_001, still = true)
        assertNotNull(state.pendingClassificationBrakeId)

        state = state.step(21_000, still = false).step(22_500, still = false)

        assertNull(state.pendingClassificationBrakeId)
        assertEquals(StopPhase.Moving, state.phase)
        assertNull(closed.single().answer)
    }

    @Test
    fun `contestar sin consulta pendiente no cambia nada ni cierra`() {
        val moving = detector.initial()
        val still = moving.step(0, still = true)
        val braking = brakingAt(0)

        assertEquals(StopStep(moving), detector.classify(moving, StopKind.PERSONAL_PAUSE))
        assertEquals(StopStep(still), detector.classify(still, StopKind.BRAKE))
        assertEquals(StopStep(braking), detector.classify(braking, StopKind.PERSONAL_PAUSE))
        assertEquals(StopStep(braking), detector.classify(braking, StopKind.BRAKE))
    }

    @Test
    fun `onRunStopped vuelve a Moving y cierra la frenada abierta sin respuesta`() {
        val state = brakingAt(0)

        val step = detector.onRunStopped(state)

        assertEquals(StopPhase.Moving, step.state.phase)
        assertEquals(ClosedStop(state.braking.brake, answer = null), step.closed)
    }

    @Test
    fun `onRunStopped no cierra una frenada ya contestada ni otras fases`() {
        val answered = brakingAt(0).step(20_001, still = true).answer(StopKind.BRAKE)
        val still = detector.initial().step(0, still = true)

        assertNull(detector.onRunStopped(answered).closed)
        assertNull(detector.onRunStopped(still).closed)
        assertNull(detector.onRunStopped(detector.initial()).closed)
    }
}

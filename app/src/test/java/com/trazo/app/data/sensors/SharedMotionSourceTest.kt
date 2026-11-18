package com.trazo.app.data.sensors

import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.repository.MotionSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedMotionSourceTest {
    private class CountingSource : MotionSource {
        val shared = MutableSharedFlow<MotionSample>()
        var registrations = 0
            private set
        var unregistrations = 0
            private set

        override fun samples(): Flow<MotionSample> =
            shared
                .onStart { registrations++ }
                .onCompletion { unregistrations++ }
    }

    private object EmptySource : MotionSource {
        override fun samples(): Flow<MotionSample> = emptyFlow()
    }

    private fun TestScope.sharedOver(upstream: MotionSource) = SharedMotionSource(upstream, backgroundScope)

    @Test
    fun `dos colectores registran el sensor una sola vez`() =
        runTest(UnconfinedTestDispatcher()) {
            val upstream = CountingSource()
            val shared = sharedOver(upstream)

            val a = launch { shared.samples().collect { } }
            val b = launch { shared.samples().collect { } }
            runCurrent()

            assertEquals(1, upstream.registrations)
            a.cancel()
            b.cancel()
        }

    @Test
    fun `al cancelar ambos colectores el sensor se da de baja`() =
        runTest(UnconfinedTestDispatcher()) {
            val upstream = CountingSource()
            val shared = sharedOver(upstream)

            val a = launch { shared.samples().collect { } }
            val b = launch { shared.samples().collect { } }
            runCurrent()
            a.cancel()
            runCurrent()
            assertEquals(0, upstream.unregistrations)

            b.cancel()
            runCurrent()

            assertEquals(1, upstream.unregistrations)
        }

    @Test
    fun `volver a colectar registra de nuevo`() =
        runTest(UnconfinedTestDispatcher()) {
            val upstream = CountingSource()
            val shared = sharedOver(upstream)

            val a = launch { shared.samples().collect { } }
            runCurrent()
            a.cancel()
            runCurrent()
            val b = launch { shared.samples().collect { } }
            runCurrent()

            assertEquals(2, upstream.registrations)
            b.cancel()
        }

    @Test
    fun `ambos colectores reciben las mismas muestras`() =
        runTest(UnconfinedTestDispatcher()) {
            val upstream = CountingSource()
            val shared = sharedOver(upstream)
            val first = mutableListOf<MotionSample>()
            val second = mutableListOf<MotionSample>()

            val a = launch { shared.samples().collect { first += it } }
            val b = launch { shared.samples().collect { second += it } }
            runCurrent()
            upstream.shared.emit(MotionSample(1, 9.8f))
            runCurrent()

            assertEquals(listOf(MotionSample(1, 9.8f)), first)
            assertEquals(first, second)
            a.cancel()
            b.cancel()
        }

    @Test
    fun `sin acelerometro el flujo queda vacio y los colectores no fallan`() =
        runTest(UnconfinedTestDispatcher()) {
            val shared = sharedOver(EmptySource)
            val received = mutableListOf<MotionSample>()

            val a = launch { shared.samples().collect { received += it } }
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(emptyList<MotionSample>(), received)
            a.cancel()
        }
}

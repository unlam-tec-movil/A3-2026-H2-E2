package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.regularity.RegularityConfig
import com.trazo.app.domain.model.regularity.SegmentClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegularityMeterTest {
    private val config = RegularityConfig()
    private val meter = RegularityMeter(config)

    /** Alimenta muestras cada 500 ms entre [from] y [to] (inclusive); devuelve los tramos cerrados. */
    private fun feed(
        from: Long,
        to: Long,
        high: Boolean,
    ): List<SegmentClass> {
        val closed = mutableListOf<SegmentClass>()
        var t = from
        var i = 0
        while (t <= to) {
            val magnitude = if (high) (if (i % 2 == 0) 5f else 15f) else 9.8f
            meter.accept(t, magnitude)?.let { closed += it }
            t += 500
            i++
        }
        return closed
    }

    @Test
    fun `10 tramos con 8 de vibracion baja cierran 8 bajos y 2 altos`() {
        val all = mutableListOf<SegmentClass>()
        repeat(10) { index ->
            val start = index * 10_500L
            all += feed(start, start + 10_000, high = index == 3 || index == 7)
        }

        assertEquals(10, all.size)
        assertEquals(8, all.count { it == SegmentClass.LOW_VIBRATION })
        assertEquals(2, all.count { it == SegmentClass.HIGH_VIBRATION })
    }

    @Test
    fun `un tramo de 9,9 segundos no se cierra`() {
        val closed = feed(0, 9_500, high = false) + listOfNotNull(meter.accept(9_900, 9.8f))

        assertEquals(emptyList<SegmentClass>(), closed)
    }

    @Test
    fun `un hueco mayor al maximo descarta el tramo abierto`() {
        feed(0, 5_000, high = false)

        // Hueco de 3 s (> 2 s): el tramo anterior se descarta y empieza uno nuevo en 8000.
        assertNull(meter.accept(8_000, 9.8f))
        // Pasaron 10 s desde el inicio original (0): si no se hubiera descartado, cerraría acá.
        assertNull(meter.accept(10_000, 9.8f))
        assertNull(meter.accept(12_000, 9.8f))
        assertNull(meter.accept(14_000, 9.8f))
        assertNull(meter.accept(16_000, 9.8f))
        assertEquals(SegmentClass.LOW_VIBRATION, meter.accept(18_000, 9.8f))
    }

    @Test
    fun `el descarte manual deja el medidor sin tramo abierto`() {
        feed(0, 9_000, high = false)
        meter.reset()

        // Si el tramo anterior siguiera abierto, esta muestra lo cerraría.
        assertNull(meter.accept(10_000, 9.8f))
        assertNull(meter.accept(11_500, 9.8f))
        assertNull(meter.accept(13_000, 9.8f))
        assertNull(meter.accept(14_500, 9.8f))
        assertNull(meter.accept(16_000, 9.8f))
        assertNull(meter.accept(17_500, 9.8f))
        assertNull(meter.accept(19_000, 9.8f))
        assertEquals(SegmentClass.LOW_VIBRATION, meter.accept(20_000, 9.8f))
    }

    @Test
    fun `el umbral clasifica a ambos lados`() {
        // Muestras alternadas 9.8 +/- d: el desvio estandar poblacional es d.
        fun classify(deviation: Float): SegmentClass {
            val local = RegularityMeter(config)
            var result: SegmentClass? = null
            var t = 0L
            var i = 0
            while (t <= 10_000) {
                result = local.accept(t, 9.8f + if (i % 2 == 0) deviation else -deviation) ?: result
                t += 500
                i++
            }
            return requireNotNull(result)
        }

        assertEquals(SegmentClass.LOW_VIBRATION, classify(1.9f))
        assertEquals(SegmentClass.HIGH_VIBRATION, classify(2.1f))
    }

    @Test
    fun `un tramo cerrado no se reporta dos veces`() {
        val first = feed(0, 10_000, high = false)
        val second = listOfNotNull(meter.accept(10_500, 9.8f))

        assertEquals(1, first.size)
        assertEquals(emptyList<SegmentClass>(), second)
    }
}

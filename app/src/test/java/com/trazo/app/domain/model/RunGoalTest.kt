package com.trazo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import com.trazo.app.domain.model.map.RunGoal as MapRunGoal

class RunGoalTest {
    @Test
    fun `KM_3 se traduce a THREE_KILOMETERS`() {
        assertEquals(MapRunGoal.THREE_KILOMETERS, RunGoal.KM_3.toMapGoal())
    }

    @Test
    fun `KM_5 se traduce a FIVE_KILOMETERS`() {
        assertEquals(MapRunGoal.FIVE_KILOMETERS, RunGoal.KM_5.toMapGoal())
    }

    @Test
    fun `KM_10 se traduce a TEN_KILOMETERS`() {
        assertEquals(MapRunGoal.TEN_KILOMETERS, RunGoal.KM_10.toMapGoal())
    }
}

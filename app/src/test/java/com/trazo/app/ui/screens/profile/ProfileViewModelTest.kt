package com.trazo.app.ui.screens.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Los casos del botón «+» pasaron a `StartRunViewModelTest` (start-run, requisito 9). */
class ProfileViewModelTest {
    @Test
    fun initialState_isEmpty() {
        val state = ProfileViewModel().uiState.value

        assertEquals(ProfileUiState(), state)
        assertFalse(state.hasRecentRuns)
        assertEquals(0, state.totalRuns)
        assertEquals(0.0, state.totalDistanceKm, 0.0)
        assertEquals(0L, state.totalTimeSeconds)
    }
}

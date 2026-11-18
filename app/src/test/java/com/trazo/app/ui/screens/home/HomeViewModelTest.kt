package com.trazo.app.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Los casos del botón «+» pasaron a `StartRunViewModelTest` (start-run, requisito 9). */
class HomeViewModelTest {
    @Test
    fun initialState_isEmpty() {
        val state = HomeViewModel().uiState.value

        assertEquals(HomeUiState(), state)
        assertFalse(state.hasRecentRuns)
    }
}

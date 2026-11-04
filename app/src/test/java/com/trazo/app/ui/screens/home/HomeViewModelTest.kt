package com.trazo.app.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel()
    }

    @Test
    fun initialState_isEmptyWithStartRunEnabledAndOptionHidden() {
        val state = viewModel.uiState.value

        assertEquals(HomeUiState(), state)
        assertFalse(state.hasRecentRuns)
        assertTrue(state.isStartRunEnabled)
        assertFalse(state.isStartRunOptionVisible)
    }

    @Test
    fun onStartRunFabClick_showsStartRunOption() {
        viewModel.onStartRunFabClick()

        assertTrue(viewModel.uiState.value.isStartRunOptionVisible)
    }

    @Test
    fun onStartRunOptionDismiss_withOptionVisible_hidesStartRunOption() {
        viewModel.onStartRunFabClick()
        assertTrue(viewModel.uiState.value.isStartRunOptionVisible)

        viewModel.onStartRunOptionDismiss()

        assertFalse(viewModel.uiState.value.isStartRunOptionVisible)
    }

    @Test
    fun onStartRunOptionClick_withOptionVisible_hidesStartRunOption() {
        viewModel.onStartRunFabClick()
        assertTrue(viewModel.uiState.value.isStartRunOptionVisible)

        viewModel.onStartRunOptionClick()

        assertFalse(viewModel.uiState.value.isStartRunOptionVisible)
    }

    @Test
    fun onStartRunFabClick_twice_hidesStartRunOptionAgain() {
        viewModel.onStartRunFabClick()
        viewModel.onStartRunFabClick()

        val state = viewModel.uiState.value
        assertFalse(state.isStartRunOptionVisible)
        assertTrue(state.isStartRunEnabled)
        assertFalse(state.hasRecentRuns)
    }
}

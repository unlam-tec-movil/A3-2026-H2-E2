package com.trazo.app.ui.screens.startrun

import com.trazo.app.domain.model.RunGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StartRunViewModelTest {
    private lateinit var viewModel: StartRunViewModel

    @Before
    fun setUp() {
        viewModel = StartRunViewModel()
    }

    @Test
    fun initialState_hasDialogHiddenAndNoGoal() {
        val state = viewModel.uiState.value

        assertFalse(state.isDialogVisible)
        assertNull(state.selectedGoal)
        assertFalse(state.isConfirmEnabled)
    }

    @Test
    fun onStartRunRequested_showsDialogWithNoGoal() {
        viewModel.onStartRunRequested()

        val state = viewModel.uiState.value
        assertTrue(state.isDialogVisible)
        assertNull(state.selectedGoal)
    }

    @Test
    fun onGoalSelected_marksGoalAndEnablesConfirm() {
        viewModel.onStartRunRequested()

        viewModel.onGoalSelected(RunGoal.KM_5)

        val state = viewModel.uiState.value
        assertEquals(RunGoal.KM_5, state.selectedGoal)
        assertTrue(state.isConfirmEnabled)
    }

    @Test
    fun onGoalSelected_withAnotherGoal_keepsOnlyTheNewOne() {
        viewModel.onStartRunRequested()
        viewModel.onGoalSelected(RunGoal.KM_3)

        viewModel.onGoalSelected(RunGoal.KM_10)

        assertEquals(RunGoal.KM_10, viewModel.uiState.value.selectedGoal)
    }

    @Test
    fun onConfirm_withoutGoal_returnsNullAndKeepsDialogOpen() {
        viewModel.onStartRunRequested()

        val result = viewModel.onConfirm()

        assertNull(result)
        assertTrue(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun onConfirm_withGoal_returnsGoalAndClosesDialog() {
        viewModel.onStartRunRequested()
        viewModel.onGoalSelected(RunGoal.KM_5)

        val result = viewModel.onConfirm()

        assertEquals(RunGoal.KM_5, result)
        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun onDismiss_closesDialogAndForgetsGoal() {
        viewModel.onStartRunRequested()
        viewModel.onGoalSelected(RunGoal.KM_3)

        viewModel.onDismiss()

        val state = viewModel.uiState.value
        assertFalse(state.isDialogVisible)
        assertNull(state.selectedGoal)
    }

    @Test
    fun onStartRunRequested_afterDismiss_startsWithNoGoal() {
        viewModel.onStartRunRequested()
        viewModel.onGoalSelected(RunGoal.KM_10)
        viewModel.onDismiss()

        viewModel.onStartRunRequested()

        assertNull(viewModel.uiState.value.selectedGoal)
    }
}

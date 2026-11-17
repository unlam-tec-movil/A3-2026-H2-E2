package com.trazo.app.ui.screens.startrun

import com.trazo.app.domain.model.LocationStatus
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
        viewModel.onStartRunRequested(LocationStatus.READY)

        val state = viewModel.uiState.value
        assertTrue(state.isDialogVisible)
        assertNull(state.selectedGoal)
    }

    @Test
    fun onGoalSelected_marksGoalAndEnablesConfirm() {
        viewModel.onStartRunRequested(LocationStatus.READY)

        viewModel.onGoalSelected(RunGoal.KM_5)

        val state = viewModel.uiState.value
        assertEquals(RunGoal.KM_5, state.selectedGoal)
        assertTrue(state.isConfirmEnabled)
    }

    @Test
    fun onGoalSelected_withAnotherGoal_keepsOnlyTheNewOne() {
        viewModel.onStartRunRequested(LocationStatus.READY)
        viewModel.onGoalSelected(RunGoal.KM_3)

        viewModel.onGoalSelected(RunGoal.KM_10)

        assertEquals(RunGoal.KM_10, viewModel.uiState.value.selectedGoal)
    }

    @Test
    fun onConfirm_withoutGoal_returnsNullAndKeepsDialogOpen() {
        viewModel.onStartRunRequested(LocationStatus.READY)

        val result = viewModel.onConfirm()

        assertNull(result)
        assertTrue(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun onConfirm_withGoal_returnsGoalAndClosesDialog() {
        viewModel.onStartRunRequested(LocationStatus.READY)
        viewModel.onGoalSelected(RunGoal.KM_5)

        val result = viewModel.onConfirm()

        assertEquals(RunGoal.KM_5, result)
        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun onDismiss_closesDialogAndForgetsGoal() {
        viewModel.onStartRunRequested(LocationStatus.READY)
        viewModel.onGoalSelected(RunGoal.KM_3)

        viewModel.onDismiss()

        val state = viewModel.uiState.value
        assertFalse(state.isDialogVisible)
        assertNull(state.selectedGoal)
    }

    @Test
    fun onStartRunRequested_afterDismiss_startsWithNoGoal() {
        viewModel.onStartRunRequested(LocationStatus.READY)
        viewModel.onGoalSelected(RunGoal.KM_10)
        viewModel.onDismiss()

        viewModel.onStartRunRequested(LocationStatus.READY)

        assertNull(viewModel.uiState.value.selectedGoal)
    }

    @Test
    fun onStartRunRequested_withPermissionDenied_doesNotOpenDialogAndKeepsIssue() {
        viewModel.onStartRunRequested(LocationStatus.PERMISSION_DENIED)

        val state = viewModel.uiState.value
        assertFalse(state.isDialogVisible)
        assertEquals(LocationStatus.PERMISSION_DENIED, state.locationIssue)
    }

    @Test
    fun onLocationIssueDismiss_clearsTheIssue() {
        viewModel.onStartRunRequested(LocationStatus.PERMISSION_DENIED)

        viewModel.onLocationIssueDismiss()

        assertNull(viewModel.uiState.value.locationIssue)
    }

    @Test
    fun onStartRunRequested_withAproximateOnly_doesNotOpenDialogAndKeepsIssue() {
        viewModel.onStartRunRequested(LocationStatus.APPROXIMATE_ONLY)

        val state = viewModel.uiState.value
        assertFalse(state.isDialogVisible)
        assertEquals(LocationStatus.APPROXIMATE_ONLY, state.locationIssue)
    }

    @Test
    fun onStartRunRequested_withDeviceLocationOff_doesNotOpenDialogAndKeepsIssue() {
        viewModel.onStartRunRequested(LocationStatus.DEVICE_LOCATION_OFF)

        val state = viewModel.uiState.value
        assertFalse(state.isDialogVisible)
        assertEquals(LocationStatus.DEVICE_LOCATION_OFF, state.locationIssue)
    }
}

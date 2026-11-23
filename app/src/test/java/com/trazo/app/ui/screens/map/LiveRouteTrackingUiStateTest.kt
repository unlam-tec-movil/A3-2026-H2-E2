package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.map.RunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué controles habilita el Mapa en cada estado del recorrido, y cuándo se tapa la alerta de frenada. */
class LiveRouteTrackingUiStateTest {
    private data class Controls(
        val canPause: Boolean,
        val canResume: Boolean,
        val canFinish: Boolean,
    )

    private fun controlsFor(
        state: RunState,
        allowed: Boolean = true,
    ): Controls {
        val uiState = LiveRouteTrackingUiState(runState = state, areControlActionsAllowed = allowed)
        return Controls(uiState.canPause, uiState.canResume, uiState.canFinish)
    }

    @Test
    fun `given each run state, when the controls are allowed, then enables the matching actions`() {
        val expected =
            mapOf(
                RunState.IDLE to Controls(canPause = false, canResume = false, canFinish = false),
                RunState.ACTIVE to Controls(canPause = true, canResume = false, canFinish = true),
                // En una frenada el recorrido sigue en curso: se puede pausar y finalizar, no reanudar.
                RunState.STOPPED to Controls(canPause = true, canResume = false, canFinish = true),
                RunState.PAUSED to Controls(canPause = false, canResume = true, canFinish = true),
                RunState.PERSONAL_PAUSED to Controls(canPause = false, canResume = true, canFinish = true),
            )

        RunState.entries.forEach { state ->
            assertEquals("estado $state", expected.getValue(state), controlsFor(state))
        }
    }

    @Test
    fun `given any run state, when the controls are not allowed, then every action is disabled`() {
        val disabled = Controls(canPause = false, canResume = false, canFinish = false)

        RunState.entries.forEach { state ->
            assertEquals("estado $state", disabled, controlsFor(state, allowed = false))
        }
    }

    @Test
    fun `given no dialog nor celebration, when checking the stop alert, then it is not suppressed`() {
        assertFalse(LiveRouteTrackingUiState(runState = RunState.ACTIVE).suppressed)
    }

    @Test
    fun `given the celebration or a finish dialog, when checking the stop alert, then it is suppressed`() {
        assertTrue(LiveRouteTrackingUiState(showGoalCelebration = true).suppressed)
        assertTrue(LiveRouteTrackingUiState(showFinishConfirmDialog = true).suppressed)
        assertTrue(LiveRouteTrackingUiState(showShortRunDialog = true).suppressed)
    }
}

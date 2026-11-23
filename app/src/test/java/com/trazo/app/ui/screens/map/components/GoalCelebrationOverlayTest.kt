package com.trazo.app.ui.screens.map.components

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalCelebrationOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var dismissals = 0
    private var finishes = 0

    private fun showOverlay(
        showCelebration: Boolean,
        goal: RunGoal?,
    ) {
        composeTestRule.setContent {
            TrazoTheme {
                GoalCelebrationOverlay(
                    showCelebration = showCelebration,
                    goal = goal,
                    onDismiss = { dismissals++ },
                    onFinish = { finishes++ },
                )
            }
        }
    }

    @Test
    fun `given the celebration is hidden, when it is composed, then nothing is displayed`() {
        // Given / When
        showOverlay(showCelebration = false, goal = RunGoal.THREE_KILOMETERS)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_celebration_title)).assertDoesNotExist()
    }

    @Test
    fun `given the celebration is visible, when it is composed, then shows the title and the reached goal`() {
        // Given / When
        showOverlay(showCelebration = true, goal = RunGoal.TEN_KILOMETERS)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_celebration_title)).assertIsDisplayed()
        val reached = context.getString(R.string.map_celebration_reached_goal, context.getString(R.string.map_goal_10km))
        composeTestRule.onNodeWithText(reached).assertIsDisplayed()
    }

    @Test
    fun `given the celebration is visible, when tapping the button, then notifies the dismissal`() {
        // Given
        showOverlay(showCelebration = true, goal = null)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.map_celebration_button)).performClick()

        // Then
        assertEquals(1, dismissals)
    }

    @Test
    fun `given the finish action, when tapping Finalizar, then notifies the finish`() {
        // Given
        showOverlay(showCelebration = true, goal = RunGoal.THREE_KILOMETERS)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish)).performClick()

        // Then
        assertEquals(1, finishes)
        assertEquals(0, dismissals)
    }

    @Test
    fun `given the celebration is visible, when tapping the background, then notifies the dismissal`() {
        // Given
        showOverlay(showCelebration = true, goal = null)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.map_celebration_title)).performClick()

        // Then
        assertEquals(1, dismissals)
        assertEquals(0, finishes)
    }

    /** Muestra la celebración, la oculta y avanza 100 ms: con fadeOut todavía se está yendo. */
    private fun hideAndAdvanceShortly() {
        var visible by mutableStateOf(true)
        composeTestRule.setContent {
            TrazoTheme {
                GoalCelebrationOverlay(
                    showCelebration = visible,
                    goal = RunGoal.FIVE_KILOMETERS,
                    onDismiss = {},
                    onFinish = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        visible = false
        // Con el reloj en pausa, el cambio se avisa a mano para que llegue a la composición.
        Snapshot.sendApplyNotifications()
        composeTestRule.mainClock.advanceTimeBy(100L)
    }

    @Test
    fun `given reduce motion is on, when the celebration hides, then it disappears without fading`() {
        // Given
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)

        // When
        hideAndAdvanceShortly()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_celebration_title)).assertDoesNotExist()
    }

    @Test
    fun `given reduce motion is off, when the celebration hides, then it fades out`() {
        // Given
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

        // When
        hideAndAdvanceShortly()

        // Then: sigue en pantalla mientras se desvanece y después desaparece.
        val title = composeTestRule.onNodeWithText(context.getString(R.string.map_celebration_title))
        title.assertExists()
        composeTestRule.mainClock.advanceTimeBy(2_000L)
        title.assertDoesNotExist()
    }
}

package com.trazo.app.ui.screens.home

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var settingsClicks = 0
    private var fabClicks = 0
    private var optionDismissals = 0
    private var optionClicks = 0

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: HomeUiState) {
        composeTestRule.setContent {
            TrazoTheme {
                HomeContent(
                    state = state,
                    onSettingsClick = { settingsClicks++ },
                    onStartRunFabClick = { fabClicks++ },
                    onStartRunOptionDismiss = { optionDismissals++ },
                    onStartRunOptionClick = { optionClicks++ },
                )
            }
        }
    }

    // 2.1, 2.3
    @Test
    fun initialState_showsEmptyRunsMessage() {
        setContent(HomeUiState())

        composeTestRule.onNodeWithText(string(R.string.home_empty_runs)).assertIsDisplayed()
    }

    // 5.1, 5.2, 5.3
    @Test
    fun topBar_showsAppName_andSettingsButtonNotifiesClick() {
        setContent(HomeUiState())

        composeTestRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.home_settings_description))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, settingsClicks)
        assertEquals(0, fabClicks)
    }

    // 3.1, 3.2, 3.5
    @Test
    fun startRunFab_isEnabledAndClickable_andNotifiesClick() {
        setContent(HomeUiState())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.home_start_run_fab_description))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, fabClicks)
    }

    // 3.3
    @Test
    fun optionVisible_showsStartRunOption_andClickNotifiesSelection() {
        setContent(HomeUiState(isStartRunOptionVisible = true))

        composeTestRule
            .onNodeWithText(string(R.string.home_start_run))
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, optionClicks)
        assertEquals(0, optionDismissals)
    }

    // 3.4
    @Test
    fun optionVisible_tapOutside_notifiesDismissAndNotSelection() {
        setContent(HomeUiState(isStartRunOptionVisible = true))
        composeTestRule.onNodeWithText(string(R.string.home_start_run)).assertIsDisplayed()

        // El toque se inyecta a nivel de sistema (como un dedo real) sobre el mensaje vacío, fuera
        // de la opción. Así lo recibe la ventana del Popup focusable y no solo la raíz de Compose.
        val messageNode = composeTestRule.onNodeWithText(string(R.string.home_empty_runs)).fetchSemanticsNode()
        val tapPoint = messageNode.positionOnScreen + Offset(messageNode.size.width / 2f, messageNode.size.height / 2f)
        injectSystemTap(tapPoint)
        composeTestRule.waitForIdle()

        assertEquals(1, optionDismissals)
        assertEquals(0, optionClicks)
    }

    private fun injectSystemTap(screenPoint: Offset) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event =
                MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, screenPoint.x, screenPoint.y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
    }
}

package com.trazo.app.ui.components.layout

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalLabelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `given each goal and no goal, when labelling it, then uses its visible text`() {
        // Given
        val goals = listOf(RunGoal.THREE_KILOMETERS, RunGoal.FIVE_KILOMETERS, RunGoal.TEN_KILOMETERS, null)
        val labels = mutableListOf<String>()

        // When
        composeTestRule.setContent { goals.forEach { labels += goalLabel(it) } }
        composeTestRule.waitForIdle()

        // Then
        val expected =
            listOf(
                context.getString(R.string.map_goal_3km),
                context.getString(R.string.map_goal_5km),
                context.getString(R.string.map_goal_10km),
                context.getString(R.string.map_goal_generic),
            )
        assertEquals(expected, labels)
    }
}

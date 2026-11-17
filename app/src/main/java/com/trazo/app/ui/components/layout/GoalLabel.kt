package com.trazo.app.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal

/** Texto visible de una meta ("3 km", "5 km", "10 km"); "tu meta" si no hay meta. */
@Composable
fun goalLabel(goal: RunGoal?): String =
    when (goal) {
        RunGoal.THREE_KILOMETERS -> stringResource(R.string.map_goal_3km)
        RunGoal.FIVE_KILOMETERS -> stringResource(R.string.map_goal_5km)
        RunGoal.TEN_KILOMETERS -> stringResource(R.string.map_goal_10km)
        null -> stringResource(R.string.map_goal_generic)
    }

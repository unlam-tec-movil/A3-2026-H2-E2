package com.trazo.app.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import kotlin.math.roundToInt

/** Texto visible de una meta a partir de sus km ("3 km", "5 km", "10 km"); "tu meta" si no hay meta. */
@Composable
fun goalLabel(goalKm: Double?): String =
    if (goalKm == null) {
        stringResource(R.string.map_goal_generic)
    } else {
        stringResource(R.string.map_goal_km, goalKm.roundToInt())
    }

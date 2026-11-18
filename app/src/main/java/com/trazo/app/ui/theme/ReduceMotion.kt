package com.trazo.app.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Verdadero cuando la escala de animaciones del sistema es 0 («Quitar animaciones»). */
internal fun isReduceMotionScale(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

/** Informa si el corredor tiene desactivadas las animaciones del sistema. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        isReduceMotionScale(
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
        )
    }
}

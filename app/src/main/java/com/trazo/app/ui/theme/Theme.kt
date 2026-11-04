package com.trazo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val TrazoLightColors =
    lightColorScheme(
        primary = Primary950,
        onPrimary = Neutral0,
        primaryContainer = Primary100,
        onPrimaryContainer = Primary950,
        inversePrimary = Primary300,
        secondary = Secondary700,
        onSecondary = Neutral0,
        secondaryContainer = Secondary200,
        onSecondaryContainer = Primary950,
        // Tertiary se usa para el modo demo
        tertiary = Info700,
        onTertiary = Neutral0,
        tertiaryContainer = Info200,
        onTertiaryContainer = Info900,
        error = Error600,
        onError = Neutral0,
        errorContainer = Error200,
        onErrorContainer = Error800,
        background = Neutral100,
        onBackground = Neutral900,
        surface = Neutral100,
        onSurface = Neutral900,
        surfaceVariant = Neutral200,
        onSurfaceVariant = Neutral700,
        surfaceContainerLowest = Neutral0,
        surfaceContainerLow = Neutral50,
        surfaceContainer = Neutral200,
        surfaceContainerHigh = Neutral300,
        surfaceContainerHighest = Neutral300,
        surfaceTint = Primary950,
        outline = Neutral600,
        outlineVariant = Neutral300,
        inverseSurface = Primary900,
        inverseOnSurface = Primary100,
        scrim = Color.Black,
    )

/** Colores que Material 3 no trae: estados del sistema y colores del mapa. */
@Immutable
data class TrazoExtendedColors(
    val success: Color = Success700,
    val successContainer: Color = Success200,
    val onSuccessContainer: Color = Success700,
    val warning: Color = Warning700,
    val warningContainer: Color = Warning200,
    val onWarningContainer: Color = Warning700,
    val info: Color = Info700,
    val infoContainer: Color = Info200,
    val onInfoContainer: Color = Info900,
    // Mapa
    val traceLive: Color = Primary950,
    val traceLiveOutline: Color = Neutral0,
    val traceSaved: Color = Primary500.copy(alpha = 0.75f),
    val brakePin: Color = Error600,
    val startPin: Color = Secondary700,
    val locationHalo: Color = Primary300.copy(alpha = 0.35f),
)

val LocalTrazoColors = staticCompositionLocalOf { TrazoExtendedColors() }

@Composable
fun TrazoTheme(content: @Composable () -> Unit) {
    // Sin color dinámico (Material You): la app conserva siempre su marca.
    CompositionLocalProvider(LocalTrazoColors provides TrazoExtendedColors()) {
        MaterialTheme(
            colorScheme = TrazoLightColors,
            typography = TrazoTypography,
            shapes = TrazoShapes,
            content = content,
        )
    }
}

object TrazoTheme {
    val extendedColors: TrazoExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalTrazoColors.current
}

/**
 * Compatibilidad con el scaffolding: MainActivity y Greeting todavía llaman a este nombre.
 * Reemplazar esas llamadas por TrazoTheme y borrar esta función.
 */
@Composable
fun ScaffoldingAndroid3Theme(content: @Composable () -> Unit) {
    TrazoTheme(content = content)
}

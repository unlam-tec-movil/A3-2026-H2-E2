package com.trazo.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

private val BarWidth = 3.dp
private val BarHeights = listOf(5.dp, 8.dp, 11.dp)
private const val EMPTY_BAR_ALPHA = 0.3f

/** Barras llenas según la señal: 3 con buena señal, 1 con señal débil y ninguna sin señal. */
internal fun filledSignalBars(quality: GpsQuality): Int =
    when (quality) {
        GpsQuality.FULL_SIGNAL -> 3
        GpsQuality.WEAK_SIGNAL -> 1
        GpsQuality.NO_SIGNAL -> 0
    }

/**
 * Indicador de señal GPS (referencia §6.11): píldora de 28 dp con barras de señal y el texto «GPS».
 * Buena señal en Success, débil en Warning y sin señal en Error. Las barras llenas también cambian
 * (3, 1 o ninguna), así que no depende solo del color; el lector de pantalla lee el estado completo.
 */
@Composable
fun GpsIndicator(
    gpsQuality: GpsQuality,
    modifier: Modifier = Modifier,
) {
    val colors = TrazoTheme.extendedColors
    val (label, containerColor, contentColor) =
        when (gpsQuality) {
            GpsQuality.FULL_SIGNAL ->
                GpsLook(stringResource(R.string.map_gps_full_signal), colors.successContainer, colors.onSuccessContainer)
            GpsQuality.WEAK_SIGNAL ->
                GpsLook(stringResource(R.string.map_gps_weak_signal), colors.warningContainer, colors.onWarningContainer)
            GpsQuality.NO_SIGNAL ->
                GpsLook(
                    stringResource(R.string.map_gps_no_signal),
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
        }

    Surface(
        modifier =
            modifier
                .height(Sizes.gpsIndicatorHeight)
                .clearAndSetSemantics { contentDescription = label },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignalBars(filledBars = filledSignalBars(gpsQuality), color = contentColor)
            Text(text = stringResource(R.string.map_gps_short), style = MaterialTheme.typography.labelMedium)
        }
    }
}

private data class GpsLook(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
private fun SignalBars(
    filledBars: Int,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BarHeights.forEachIndexed { index, barHeight ->
            val filled = index < filledBars
            Box(
                modifier =
                    Modifier
                        .width(BarWidth)
                        .height(barHeight)
                        .background(
                            color = if (filled) color else color.copy(alpha = EMPTY_BAR_ALPHA),
                            shape = MaterialTheme.shapes.extraSmall,
                        ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsIndicatorFullPreview() {
    TrazoTheme { GpsIndicator(gpsQuality = GpsQuality.FULL_SIGNAL) }
}

@Preview(showBackground = true)
@Composable
private fun GpsIndicatorWeakPreview() {
    TrazoTheme { GpsIndicator(gpsQuality = GpsQuality.WEAK_SIGNAL) }
}

@Preview(showBackground = true)
@Composable
private fun GpsIndicatorNoSignalPreview() {
    TrazoTheme { GpsIndicator(gpsQuality = GpsQuality.NO_SIGNAL) }
}

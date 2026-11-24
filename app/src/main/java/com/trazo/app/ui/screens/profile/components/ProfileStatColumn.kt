package com.trazo.app.ui.screens.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.rememberReduceMotion
import com.trazo.app.ui.theme.tabular

private val BarWidth = 56.dp
private val BarMinHeight = 12.dp
private val BarMaxHeight = 48.dp
private const val BAR_ANIMATION_MILLIS = 600

/**
 * Una métrica de «Mis estadísticas»: una barra que crece hacia arriba según [fill] (de 0 a 1, ya
 * calculado por el ViewModel) y no pasa de un tope chico; debajo, el [value] tabular y la [label].
 * La barra es decorativa: el lector de pantalla lee la columna como un solo elemento
 * ("Distancia total, 0,0 km"). Con «Quitar animaciones» la barra cambia sin animar.
 */
@Composable
fun ProfileStatColumn(
    label: String,
    value: String,
    fill: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val animatedFill by animateFloatAsState(
        targetValue = fill,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = BAR_ANIMATION_MILLIS),
        label = "statBarFill",
    )

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(BarMaxHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(BarWidth)
                        .height(lerp(BarMinHeight, BarMaxHeight, animatedFill))
                        .background(color = barColor),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfileStatColumnPreview() {
    TrazoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ProfileStatColumn(
                label = "Recorridos guardados",
                value = "0",
                fill = 0f,
                barColor = TrazoTheme.extendedColors.statBarRuns,
                modifier = Modifier.weight(1f),
            )
            ProfileStatColumn(
                label = "Distancia total",
                value = "25,0 km",
                fill = 0.5f,
                barColor = TrazoTheme.extendedColors.statBarDistance,
                modifier = Modifier.weight(1f),
            )
            ProfileStatColumn(
                label = "Tiempo total",
                value = "10:00:00",
                fill = 1f,
                barColor = TrazoTheme.extendedColors.statBarTime,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

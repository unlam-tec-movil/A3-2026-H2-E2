package com.trazo.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.ui.theme.Spacing

private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.8f

/** Atribución obligatoria de OpenStreetMap y CARTO, sobre un fondo translúcido. La usan todos los mapas de Trazo. */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.map_attribution),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                    shape = MaterialTheme.shapes.extraSmall,
                ).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

package com.trazo.app.ui.screens.map.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.ui.screens.map.BrakeAlertUi
import com.trazo.app.ui.screens.map.StopDetectionUiState
import com.trazo.app.ui.screens.map.StopDetectionViewModel
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Superposición de la detección de frenadas: alerta y diálogo de consulta.
 *
 * Con [suppressed] verdadero (cierre del recorrido, HU-06) no se muestra ninguno de los dos.
 */
@Composable
fun StopDetectionOverlay(
    suppressed: Boolean,
    modifier: Modifier = Modifier,
    viewModel: StopDetectionViewModel = hiltViewModel(),
) {
    LaunchedEffect(suppressed) { viewModel.onSuppressedChanged(suppressed) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StopDetectionOverlayContent(
        uiState = uiState,
        suppressed = suppressed,
        onClassify = viewModel::onClassify,
        modifier = modifier,
    )
}

/** Parte visual sin estado propio: dibuja según [uiState] y [suppressed]. */
@Composable
internal fun StopDetectionOverlayContent(
    uiState: StopDetectionUiState,
    suppressed: Boolean,
    onClassify: (StopKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Con supresión no se dibuja nada, aunque el estado del ViewModel aún no se haya actualizado.
    if (suppressed) return
    Box(modifier = modifier) {
        BrakeAlert(
            visible = uiState.alert != null,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        if (uiState.classificationVisible) {
            StopClassificationDialog(onClassify = onClassify)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StopDetectionOverlayAlertPreview() {
    TrazoTheme {
        StopDetectionOverlayContent(
            uiState = StopDetectionUiState(alert = BrakeAlertUi(brakeId = "b1")),
            suppressed = false,
            onClassify = {},
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StopDetectionOverlayDialogPreview() {
    TrazoTheme {
        StopDetectionOverlayContent(
            uiState = StopDetectionUiState(classificationVisible = true),
            suppressed = false,
            onClassify = {},
        )
    }
}

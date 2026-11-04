package com.trazo.app.ui.screens.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.trazo.app.R
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/** Opacidades de Material 3 para un contenedor y un contenido deshabilitados. */
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/** Elevación level2 de Material 3 (opción "Iniciar un recorrido", referencia §5.4). */
private val OptionElevationLevel2 = 3.dp

/**
 * FAB "+" de Inicio. Al tocarlo, la pantalla decide mostrar la opción "Iniciar un recorrido",
 * que se dibuja en un [Popup] focusable: cualquier toque fuera de ella la cierra.
 * No guarda estado propio; todos los eventos salen por parámetro.
 */
@Composable
fun StartRunFab(
    enabled: Boolean,
    isOptionVisible: Boolean,
    onFabClick: () -> Unit,
    onOptionDismiss: () -> Unit,
    onOptionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (enabled) {
            FloatingActionButton(
                onClick = onFabClick,
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                // Los valores por defecto de Material 3 son level3.
                elevation = FloatingActionButtonDefaults.elevation(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.home_start_run_fab_description),
                )
            }
        } else {
            DisabledFab()
        }

        if (enabled && isOptionVisible) {
            StartRunOption(
                onDismiss = onOptionDismiss,
                onClick = onOptionClick,
            )
        }
    }
}

/**
 * [FloatingActionButton] no tiene estado deshabilitado: se dibuja con los colores de deshabilitado
 * de Material 3, sin `clickable` (no recibe toques ni foco) y con semántica de botón deshabilitado.
 */
@Composable
private fun DisabledFab() {
    val description = stringResource(R.string.home_start_run_fab_description)
    val onSurface = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier =
            Modifier
                .defaultMinSize(minWidth = Sizes.runControl, minHeight = Sizes.runControl)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                    disabled()
                },
        shape = MaterialTheme.shapes.large,
        color = onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
        contentColor = onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun StartRunOption(
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val gapPx = with(LocalDensity.current) { Spacing.lg.roundToPx() }
    val positionProvider = remember(gapPx) { AboveAnchorEndAlignedPositionProvider(gapPx) }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_route),
                    contentDescription = null,
                )
            },
            text = { Text(text = stringResource(R.string.home_start_run)) },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = OptionElevationLevel2,
                    pressedElevation = OptionElevationLevel2,
                    focusedElevation = OptionElevationLevel2,
                    hoveredElevation = OptionElevationLevel2,
                ),
        )
    }
}

/**
 * Ubica el popup con su borde derecho alineado al del ancla (el FAB) y su borde inferior
 * [gapPx] por encima del ancla, sin salirse de la ventana.
 */
private class AboveAnchorEndAlignedPositionProvider(
    private val gapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x =
            (anchorBounds.right - popupContentSize.width)
                .coerceAtMost(windowSize.width - popupContentSize.width)
                .coerceAtLeast(0)
        val y =
            (anchorBounds.top - gapPx - popupContentSize.height)
                .coerceAtMost(windowSize.height - popupContentSize.height)
                .coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Preview(showBackground = true)
@Composable
private fun StartRunFabClosedPreview() {
    TrazoTheme {
        StartRunFab(
            enabled = true,
            isOptionVisible = false,
            onFabClick = {},
            onOptionDismiss = {},
            onOptionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 240)
@Composable
private fun StartRunFabOpenPreview() {
    TrazoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            StartRunFab(
                enabled = true,
                isOptionVisible = true,
                onFabClick = {},
                onOptionDismiss = {},
                onOptionClick = {},
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartRunFabDisabledPreview() {
    TrazoTheme {
        StartRunFab(
            enabled = false,
            isOptionVisible = false,
            onFabClick = {},
            onOptionDismiss = {},
            onOptionClick = {},
        )
    }
}

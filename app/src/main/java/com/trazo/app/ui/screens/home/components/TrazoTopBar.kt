package com.trazo.app.ui.screens.home.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Barra superior con fondo primary, el [title] a la izquierda y el botón de Configuración a la derecha.
 * Ocupa también el área de la barra de estado (inset por defecto de [TopAppBar]).
 *
 * Nace en Inicio; se mueve a `ui/components/layout/` cuando la use otra pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrazoTopBar(
    title: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_settings_description),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun TrazoTopBarHomePreview() {
    TrazoTheme {
        TrazoTopBar(
            title = stringResource(R.string.app_name),
            onSettingsClick = {},
        )
    }
}

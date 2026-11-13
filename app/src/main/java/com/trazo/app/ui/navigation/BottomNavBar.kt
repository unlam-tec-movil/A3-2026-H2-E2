package com.trazo.app.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Barra de navegación inferior con las 3 pestañas de Trazo.
 *
 * El ítem activo se distingue por el indicador primaryContainer, el ícono relleno y la
 * etiqueta en negrita (no solo por color). Los íconos no tienen descripción porque la
 * etiqueta ya da el nombre al lector de pantalla.
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onDestinationSelected: (TrazoDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    NavigationBar(
        modifier = modifier,
        containerColor = colorScheme.onPrimary,
    ) {
        TrazoDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    val iconRes =
                        if (selected) destination.selectedIconRes else destination.unselectedIconRes
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                enabled = true,
                alwaysShowLabel = true,
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = colorScheme.primaryContainer,
                        selectedIconColor = colorScheme.onPrimaryContainer,
                        selectedTextColor = colorScheme.onSurface,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavBarHomeSelectedPreview() {
    TrazoTheme {
        BottomNavBar(
            currentRoute = TrazoDestination.HOME.route,
            onDestinationSelected = {},
        )
    }
}

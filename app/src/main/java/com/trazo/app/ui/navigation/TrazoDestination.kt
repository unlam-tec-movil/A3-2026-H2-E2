package com.trazo.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.trazo.app.R

/**
 * Las 3 pestañas principales de la app y su ruta de navegación.
 * El orden del enum define el orden de la barra de navegación.
 */
enum class TrazoDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
    @param:DrawableRes val unselectedIconRes: Int,
) {
    HOME("home", R.string.nav_home, R.drawable.ic_nav_home_filled, R.drawable.ic_nav_home_outlined),
    MAP("map", R.string.nav_map, R.drawable.ic_nav_map_filled, R.drawable.ic_nav_map_outlined),
    HISTORY("history", R.string.nav_history, R.drawable.ic_nav_history_filled, R.drawable.ic_nav_history_outlined),
}

package com.trazo.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.trazo.app.R

/**
 * Las 2 pestañas principales de la app y su ruta de navegación.
 * El orden del enum define el orden de la barra de navegación.
 */
enum class TrazoDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
    @param:DrawableRes val unselectedIconRes: Int,
) {
    ACTIVITY("activity", R.string.nav_activity, R.drawable.ic_nav_activity_filled, R.drawable.ic_nav_activity_outlined),
    PROFILE("profile", R.string.nav_profile, R.drawable.ic_nav_profile_filled, R.drawable.ic_nav_profile_outlined),
}

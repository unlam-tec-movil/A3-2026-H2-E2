package com.trazo.app.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.trazo.app.domain.model.LocationStatus

/** Arma el Intent que abre la Configuración que corresponde a cada problema. */
fun locationSettingsIntent(
    context: Context,
    status: LocationStatus,
): Intent =
    if (status == LocationStatus.DEVICE_LOCATION_OFF) {
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    }

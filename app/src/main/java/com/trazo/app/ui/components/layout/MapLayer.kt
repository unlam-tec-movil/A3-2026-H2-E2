package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.trazo.app.domain.model.map.RoutePoint

@Composable
fun MapLayer(
    routePoints: List<RoutePoint>,
    currentLocation: LatLng?,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState()

    var hasCenteredInitially by remember {
        mutableStateOf(false)
    }

    val markerState =
        remember {
            MarkerState(
                position = LatLng(0.0, 0.0),
            )
        }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            markerState.position = location

            if (!hasCenteredInitially) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        location,
                        17f,
                    ),
                )
                hasCenteredInitially = true
            }
        }
    }

    val routeCoordinates =
        remember(routePoints) {
            routePoints.map { point ->
                LatLng(
                    point.latitude,
                    point.longitude,
                )
            }
        }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    isMyLocationEnabled = false,
                ),
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true,
                    mapToolbarEnabled = false,
                ),
        ) {
            if (currentLocation != null) {
                Marker(
                    state = markerState,
                    title = "Tu ubicación",
                )
            }

            if (routeCoordinates.size >= 2) {
                Polyline(
                    points = routeCoordinates,
                    width = 12f,
                )
            }
        }

        SmallFloatingActionButton(
            onClick = {
                currentLocation?.let { location ->
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            location,
                            17f,
                        ),
                    )
                }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recentrar mapa",
            )
        }
    }
}

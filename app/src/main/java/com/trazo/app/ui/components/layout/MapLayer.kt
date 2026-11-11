package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

@Composable
fun MapLayer(
    routePoints: List<RoutePoint>,
    currentLocation: LatLng?,
    isMapCenteredOnRunner: Boolean,
    onCenterMapClicked: () -> Unit,
    onMapDragged: () -> Unit,
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

    LaunchedEffect(currentLocation, isMapCenteredOnRunner) {
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
            } else if (isMapCenteredOnRunner) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(location),
                )
            }
        }
    }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving to cameraPositionState.cameraMoveStartedReason }
            .collect { (isMoving, reason) ->
                if (isMoving && reason == CameraMoveStartedReason.GESTURE) {
                    onMapDragged()
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

    val density = LocalDensity.current
    val traceWidth = with(density) { Sizes.traceLiveWidth.toPx() }
    val traceOutlineWidth = with(density) { (Sizes.traceLiveWidth + 2.dp).toPx() }

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
                    color = TrazoTheme.extendedColors.traceLiveOutline,
                    width = traceOutlineWidth,
                )

                Polyline(
                    points = routeCoordinates,
                    color = TrazoTheme.extendedColors.traceLive,
                    width = traceWidth,
                )
            }
        }

        SmallFloatingActionButton(
            onClick = {
                onCenterMapClicked()
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
                    .padding(Spacing.lg)
                    .size(Sizes.minTouchTarget),
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

package com.trazo.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.ui.navigation.appDetailsSettingsIntent
import com.trazo.app.ui.screens.settings.components.DemoModeSwitch
import com.trazo.app.ui.screens.settings.components.PermissionRow
import com.trazo.app.ui.screens.settings.components.cameraPermissionStatusText
import com.trazo.app.ui.screens.settings.components.locationPermissionStatusText
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onRefreshPermissions()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsContent(
        state = state,
        onBackClick = onBackClick,
        onLocationSettingsClick = {
            context.startActivity(appDetailsSettingsIntent(context))
        },
        onCameraSettingsClick = {
            context.startActivity(appDetailsSettingsIntent(context))
        },
        onDemoModeToggle = viewModel::onDemoModeToggled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBackClick: () -> Unit,
    onLocationSettingsClick: () -> Unit,
    onCameraSettingsClick: () -> Unit,
    onDemoModeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        ) {
            Text(
                text = stringResource(R.string.settings_permissions_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            PermissionRow(
                label = stringResource(R.string.settings_location_label),
                statusText = locationPermissionStatusText(state.locationStatus),
                showSettingsButton = state.showLocationSettingsButton,
                onSettingsClick = onLocationSettingsClick,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            PermissionRow(
                label = stringResource(R.string.settings_camera_label),
                statusText = cameraPermissionStatusText(state.cameraPermissionStatus),
                showSettingsButton = state.showCameraSettingsButton,
                onSettingsClick = onCameraSettingsClick,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.settings_demo_mode_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            DemoModeSwitch(
                state = state,
                onDemoModeToggle = onDemoModeToggle,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SettingsContentPreview() {
    TrazoTheme {
        SettingsContent(
            state =
                SettingsUiState(
                    locationStatus = LocationStatus.READY,
                    cameraPermissionStatus = CameraPermissionStatus.DENIED,
                    isDemoModeEnabled = true,
                    isRunInProgress = false,
                ),
            onBackClick = {},
            onLocationSettingsClick = {},
            onCameraSettingsClick = {},
            onDemoModeToggle = {},
        )
    }
}

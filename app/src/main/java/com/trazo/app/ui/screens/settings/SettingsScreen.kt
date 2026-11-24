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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.trazo.app.ui.screens.settings.components.PermissionNotice
import com.trazo.app.ui.screens.settings.components.PermissionRow
import com.trazo.app.ui.screens.settings.components.SettingsGroup
import com.trazo.app.ui.screens.settings.components.SettingsSectionHeader
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
        onOpenSystemSettingsClick = {
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
    onOpenSystemSettingsClick: () -> Unit,
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
            SettingsSectionHeader(text = stringResource(R.string.settings_permissions_section))

            Spacer(modifier = Modifier.height(Spacing.sm))

            SettingsGroup {
                PermissionRow(
                    icon = Icons.Filled.LocationOn,
                    label = stringResource(R.string.settings_location_label),
                    subtitle = stringResource(R.string.settings_location_subtitle),
                    statusText = locationPermissionStatusText(state.locationStatus),
                    onClick = onOpenSystemSettingsClick,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PermissionRow(
                    icon = Icons.Filled.PhotoCamera,
                    label = stringResource(R.string.settings_camera_label),
                    subtitle = stringResource(R.string.settings_camera_subtitle),
                    statusText = cameraPermissionStatusText(state.cameraPermissionStatus),
                    onClick = onOpenSystemSettingsClick,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            PermissionNotice(
                arePermissionsGranted = state.arePermissionsGranted,
                onOpenSystemSettingsClick = onOpenSystemSettingsClick,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            SettingsSectionHeader(text = stringResource(R.string.settings_advanced_section))

            Spacer(modifier = Modifier.height(Spacing.sm))

            DemoModeSwitch(
                state = state,
                onDemoModeToggle = onDemoModeToggle,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun SettingsContentRestrictedPreview() {
    TrazoTheme {
        SettingsContent(
            state =
                SettingsUiState(
                    locationStatus = LocationStatus.APPROXIMATE_ONLY,
                    cameraPermissionStatus = CameraPermissionStatus.DENIED,
                ),
            onBackClick = {},
            onOpenSystemSettingsClick = {},
            onDemoModeToggle = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun SettingsContentGrantedPreview() {
    TrazoTheme {
        SettingsContent(
            state =
                SettingsUiState(
                    locationStatus = LocationStatus.READY,
                    cameraPermissionStatus = CameraPermissionStatus.GRANTED,
                    isDemoModeEnabled = true,
                ),
            onBackClick = {},
            onOpenSystemSettingsClick = {},
            onDemoModeToggle = {},
        )
    }
}

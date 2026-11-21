package com.trazo.app.data.repository

import com.trazo.app.data.local.DemoModePreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DemoModeRepositoryImpl
    @Inject
    constructor(
        private val demoModePreferences: DemoModePreferences,
    ) : DemoModeRepository {
        override val isDemoModeEnabled: Flow<Boolean> =
            demoModePreferences.isDemoModeEnabled

        override suspend fun setDemoModeEnabled(enabled: Boolean) {
            demoModePreferences.setDemoModeEnabled(enabled)
        }
    }

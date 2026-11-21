package com.trazo.app.data.local

import kotlinx.coroutines.flow.Flow

interface DemoModePreferences {
    val isDemoModeEnabled: Flow<Boolean>

    suspend fun setDemoModeEnabled(enabled: Boolean)
}

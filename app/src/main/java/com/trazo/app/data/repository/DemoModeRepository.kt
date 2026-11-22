package com.trazo.app.data.repository

import kotlinx.coroutines.flow.Flow

interface DemoModeRepository {
    val isDemoModeEnabled: Flow<Boolean>

    suspend fun setDemoModeEnabled(enabled: Boolean)
}

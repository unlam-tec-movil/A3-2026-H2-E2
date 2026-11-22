package com.trazo.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDemoModeRepository(
    initialValue: Boolean = false,
) : DemoModeRepository {
    private val _isDemoModeEnabled = MutableStateFlow(initialValue)
    override val isDemoModeEnabled: StateFlow<Boolean> = _isDemoModeEnabled.asStateFlow()

    var setDemoModeCalls = mutableListOf<Boolean>()

    override suspend fun setDemoModeEnabled(enabled: Boolean) {
        setDemoModeCalls.add(enabled)
        _isDemoModeEnabled.value = enabled
    }
}

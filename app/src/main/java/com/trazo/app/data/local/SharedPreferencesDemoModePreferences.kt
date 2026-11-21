package com.trazo.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesDemoModePreferences
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : DemoModePreferences {
        private val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        private val _isDemoModeEnabled = MutableStateFlow(false)
        override val isDemoModeEnabled: Flow<Boolean> = _isDemoModeEnabled.asStateFlow()

        init {
            _isDemoModeEnabled.value = prefs.getBoolean(KEY_DEMO_MODE, false)
        }

        override suspend fun setDemoModeEnabled(enabled: Boolean) {
            prefs.edit { putBoolean(KEY_DEMO_MODE, enabled) }
            _isDemoModeEnabled.value = enabled
        }

        private companion object {
            const val PREFS_NAME = "trazo_preferences"
            const val KEY_DEMO_MODE = "key_demo_mode_enabled"
        }
    }

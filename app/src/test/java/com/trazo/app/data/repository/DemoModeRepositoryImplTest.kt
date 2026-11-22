package com.trazo.app.data.repository

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoModeRepositoryImplTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.getSharedPreferences("trazo_preferences", Context.MODE_PRIVATE).edit { clear() }
    }

    @Test
    fun `isDemoModeEnabled returns false by default when not configured`() =
        runTest {
            val repository = DemoModeRepositoryImpl(context)

            assertFalse(repository.isDemoModeEnabled.first())
        }

    @Test
    fun `isDemoModeEnabled returns true when previously saved as true`() =
        runTest {
            context.getSharedPreferences("trazo_preferences", Context.MODE_PRIVATE).edit {
                putBoolean("key_demo_mode_enabled", true)
            }

            val repository = DemoModeRepositoryImpl(context)

            assertTrue(repository.isDemoModeEnabled.first())
        }

    @Test
    fun `setDemoModeEnabled updates flow and persists value`() =
        runTest {
            val repository = DemoModeRepositoryImpl(context)

            assertFalse(repository.isDemoModeEnabled.first())

            repository.setDemoModeEnabled(true)
            assertTrue(repository.isDemoModeEnabled.first())

            val prefs = context.getSharedPreferences("trazo_preferences", Context.MODE_PRIVATE)
            assertTrue(prefs.getBoolean("key_demo_mode_enabled", false))

            repository.setDemoModeEnabled(false)
            assertFalse(repository.isDemoModeEnabled.first())
            assertFalse(prefs.getBoolean("key_demo_mode_enabled", true))
        }
}

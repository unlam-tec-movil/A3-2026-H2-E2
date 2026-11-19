package com.trazo.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccelerometerMotionSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun conUnColectorActivoLlegaUnaMuestraConMagnitudPositiva() =
        runBlocking {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            assumeTrue(manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)

            val sample = withTimeout(5_000) { AccelerometerMotionSource(context).samples().first() }

            assertTrue(sample.magnitude > 0f)
        }
}

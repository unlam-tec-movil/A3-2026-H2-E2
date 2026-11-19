package com.trazo.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSensor
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
class AccelerometerMotionSourceSamplesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Test
    fun `given a device without accelerometer, when collecting samples, then the flow is empty`() =
        runBlocking {
            // Given
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { shadowOf(manager).removeSensor(it) }

            // When
            val samples = AccelerometerMotionSource(context).samples().toList()

            // Then
            assertEquals(0, samples.size)
        }

    @Test
    fun `given an accelerometer, when it reports a reading, then emits the magnitude and the timestamp in millis`() =
        runBlocking {
            // Given
            val sensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
            shadowOf(manager).addSensor(sensor)
            val firstSample =
                async(start = CoroutineStart.UNDISPATCHED) {
                    AccelerometerMotionSource(context).samples().first()
                }
            yield()

            // When
            val event =
                ReflectionHelpers.callConstructor(
                    SensorEvent::class.java,
                    ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, 3),
                )
            event.sensor = sensor
            event.values[0] = 3f
            event.values[1] = 4f
            event.values[2] = 0f
            event.timestamp = 2_000_000L
            shadowOf(manager).sendSensorEventToListeners(event)
            val sample = firstSample.await()

            // Then
            assertEquals(5f, sample.magnitude, 0.001f)
            assertEquals(2L, sample.timestampMillis)
        }
}

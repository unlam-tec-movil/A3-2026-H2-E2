package com.trazo.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMotionSensorAvailabilityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun coincideConLaPresenciaRealDelAcelerometroYNoLanzaExcepciones() {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val expected = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        assertEquals(expected, AndroidMotionSensorAvailability(context).isAvailable())
    }
}

package com.trazo.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Hay acelerómetro solo si existe el sensor; sin gestor de sensores, no hay. */
class AndroidMotionSensorAvailability
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MotionSensorAvailability {
        override fun isAvailable(): Boolean {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            return manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        }
    }

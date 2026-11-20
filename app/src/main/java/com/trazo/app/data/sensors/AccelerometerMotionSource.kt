package com.trazo.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.trazo.app.domain.model.stops.MotionSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import kotlin.math.sqrt

/** Acelerómetro como flujo frío: registra el sensor al colectar y lo da de baja al cancelar. */
class AccelerometerMotionSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MotionSource {
        override fun samples(): Flow<MotionSample> {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (manager == null || sensor == null) {
                Log.w(TAG, "Sin acelerómetro: la detección de frenadas queda inactiva")
                return emptyFlow()
            }
            return callbackFlow {
                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            val (x, y, z) = event.values
                            trySend(
                                MotionSample(
                                    timestampMillis = event.timestamp / NANOS_PER_MILLI,
                                    magnitude = sqrt(x * x + y * y + z * z),
                                ),
                            )
                        }

                        override fun onAccuracyChanged(
                            sensor: Sensor?,
                            accuracy: Int,
                        ) = Unit
                    }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                awaitClose { manager.unregisterListener(listener) }
            }
        }

        private companion object {
            const val TAG = "AccelerometerMotionSource"
            const val NANOS_PER_MILLI = 1_000_000L
        }
    }

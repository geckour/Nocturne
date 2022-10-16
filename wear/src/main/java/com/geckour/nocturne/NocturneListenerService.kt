package com.geckour.nocturne

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import timber.log.Timber

class NocturneListenerService : WearableListenerService() {

    companion object {

        private const val DATA_PATH_BACKGROUND_IMAGE = "/data/background_image"

        internal const val PREFERENCE_KEY_BACKGROUND_IMAGE = "preference_key_background_image"
    }

    private lateinit var sharedPreferences: SharedPreferences

    private val sensorEventListener = object : SensorEventListener2 {
        override fun onSensorChanged(event: SensorEvent?) {
            Timber.d("ngeck called 1")
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Timber.d("ngeck called 2")
        }

        override fun onFlushCompleted(sensor: Sensor?) {
            Timber.d("ngeck called 3")
        }
    }

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        getSystemService<SensorManager>()?.apply {
            registerListener(sensorEventListener, getDefaultSensor(Sensor.TYPE_HEART_RATE), SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        getSystemService<SensorManager>()?.unregisterListener(sensorEventListener)

        super.onDestroy()
    }

    override fun onDataChanged(buffer: DataEventBuffer) {
        super.onDataChanged(buffer)

        Timber.d("ngeck called")
        buffer.forEach { event ->
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    if (event.dataItem.uri.path == DATA_PATH_BACKGROUND_IMAGE) {
                        sharedPreferences.edit {
                            putString(
                                PREFERENCE_KEY_BACKGROUND_IMAGE,
                                DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray("value")?.toString()
                            )
                        }
                    }
                }
                DataEvent.TYPE_DELETED -> {
                    if (event.dataItem.uri.path == DATA_PATH_BACKGROUND_IMAGE) {
                        sharedPreferences.edit {
                            remove(PREFERENCE_KEY_BACKGROUND_IMAGE)
                        }
                    }
                }
            }
        }
    }
}
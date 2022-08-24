package com.geckour.nocturne

import android.view.SurfaceHolder
import android.widget.Toast
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class NocturneFaceService : WatchFaceService(), DataClient.OnDataChangedListener {

    companion object {

        private const val PATH_ALARM_TIME = "/path_alarm_time"
        private const val KEY_ALARM_TIME = "value"
    }

    private val info = Info(null)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = NocturneWatchCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )

        // Creates the watch face.
        return WatchFace(
            watchFaceType = WatchFaceType.ANALOG,
            renderer = renderer
        ).setTapListener(object : WatchFace.TapListener {

            override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                when (tapType) {
                    TapType.DOWN -> {
                        // The user has started touching the screen.
                    }

                    TapType.CANCEL -> {
                        // The user has started a different gesture or otherwise cancelled the tap.
                    }

                    TapType.UP ->
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.message_moonage, ZonedDateTime.now().moonAge()),
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        })
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        dataEventBuffer.forEach { dataEvent ->
            when (dataEvent.type) {
                DataEvent.TYPE_CHANGED -> {
                    if (dataEvent.dataItem.uri.path?.compareTo(PATH_ALARM_TIME) == 0) {
                        val dataMap = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap

                        val alarmTime = dataMap.getLong(KEY_ALARM_TIME).apply { Timber.d("ngeck alarm time: $this") }
                        try {
                            info.nextAlarmTime = ZonedDateTime.of(
                                LocalDateTime.ofEpochSecond(alarmTime, 0, ZoneOffset.of(ZoneOffset.systemDefault().id)),
                                ZoneId.systemDefault()
                            )
                        } catch (t: Throwable) {
                            Timber.e(t)
                        }
                    }
                }

                DataEvent.TYPE_DELETED -> {
                    try {
                        info.nextAlarmTime = null
                    } catch (t: Throwable) {
                        Timber.e(t)
                    }
                }
            }
        }
    }
}

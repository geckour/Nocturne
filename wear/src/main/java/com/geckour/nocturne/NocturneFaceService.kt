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
import java.time.ZonedDateTime

class NocturneFaceService : WatchFaceService() {

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
}

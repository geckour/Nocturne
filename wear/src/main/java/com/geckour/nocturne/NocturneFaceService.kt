package com.geckour.nocturne

import android.content.Context
import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class NocturneFaceService : WatchFaceService() {

    companion object {

        private const val DATA_PATH_BACKGROUND_IMAGE = "/data/background_image"

        val fillWaveSetting =
            UserStyleSetting.BooleanUserStyleSetting(
                id = UserStyleSetting.Id("settings_fill_wave"),
                displayName = "Fill wave",
                description = "",
                icon = null,
                affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS),
                defaultValue = true
            )
        val circleTypeSetting =
            UserStyleSetting.LongRangeUserStyleSetting(
                id = UserStyleSetting.Id("settings_circle_type"),
                displayName = "Type of circle",
                description = "",
                icon = null,
                affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS),
                defaultValue = CircleType.default.ordinal.toLong(),
                minimumValue = CircleType.values().minOf { it.ordinal }.toLong(),
                maximumValue = CircleType.values().maxOf { it.ordinal }.toLong()
            )
    }

    override fun createUserStyleSchema(): UserStyleSchema =
        UserStyleSchema(
            listOf(
                fillWaveSetting,
                circleTypeSetting,
            )
        )

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

        val backgroundImageBytes = Wearable.getDataClient(this)
            .dataItems
            .await()
            .first { it.uri.path == DATA_PATH_BACKGROUND_IMAGE }
            .let { dataItem ->
                DataMapItem.fromDataItem(dataItem)
                    .dataMap
                    .getAsset("value")
                    ?.toByteArray(this)
            }

        renderer.setBackground(backgroundImageBytes)

        return WatchFace(
            watchFaceType = WatchFaceType.ANALOG,
            renderer = renderer
        ).setTapListener(
            object : WatchFace.TapListener {

                override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                    when (tapType) {
                        TapType.DOWN -> {
                            // The user has started touching the screen.
                        }

                        TapType.CANCEL -> {
                            // The user has started a different gesture or otherwise cancelled the tap.
                        }

                        TapType.UP -> {
                            renderer.showMoonAge()
                        }
                    }
                }
            }
        )
    }

    override fun createComplicationSlotsManager(currentUserStyleRepository: CurrentUserStyleRepository): ComplicationSlotsManager {
        val defaultCanvasComplicationFactory =
            CanvasComplicationFactory { watchState, invalidateCallback ->
                CanvasComplicationDrawable(
                    checkNotNull(ComplicationDrawable.getDrawable(this, R.drawable.complication)),
                    watchState,
                    invalidateCallback
                )
            }
        val dataSourcePolicy = DefaultComplicationDataSourcePolicy()

        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    id = 0,
                    defaultCanvasComplicationFactory,
                    listOf(
                        ComplicationType.SMALL_IMAGE,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.NO_DATA,
                        ComplicationType.EMPTY,
                        ComplicationType.NOT_CONFIGURED,
                    ),
                    dataSourcePolicy,
                    ComplicationSlotBounds(RectF(0.4f, 0.125f, 0.6f, 0.325f))
                ).build(),
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    id = 1,
                    defaultCanvasComplicationFactory,
                    listOf(
                        ComplicationType.SMALL_IMAGE,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.NO_DATA,
                        ComplicationType.EMPTY,
                        ComplicationType.NOT_CONFIGURED,
                    ),
                    dataSourcePolicy,
                    ComplicationSlotBounds(RectF(0.2f, 0.15f, 0.4f, 0.35f))
                ).build(),
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    id = 2,
                    defaultCanvasComplicationFactory,
                    listOf(
                        ComplicationType.SMALL_IMAGE,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.NO_DATA,
                        ComplicationType.EMPTY,
                        ComplicationType.NOT_CONFIGURED,
                    ),
                    dataSourcePolicy,
                    ComplicationSlotBounds(RectF(0.6f, 0.15f, 0.8f, 0.35f))
                ).build(),
            ),
            currentUserStyleRepository
        )
    }

    private suspend fun Asset.toByteArray(context: Context): ByteArray =
        Wearable.getDataClient(context)
            .getFdForAsset(this@toByteArray)
            .await()
            .inputStream
            .readBytes()
}

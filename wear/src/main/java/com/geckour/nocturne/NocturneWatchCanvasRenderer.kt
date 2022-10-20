package com.geckour.nocturne

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class NocturneWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    private val currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<Renderer.SharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    true
) {

    private val spec: Size = Point().apply { context.getSystemService<WindowManager>()?.defaultDisplay?.getSize(this) }
        .let {
            Size(
                View.MeasureSpec.makeMeasureSpec(it.x, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(it.y, View.MeasureSpec.EXACTLY)
            )
        }

    private val layout: View = LayoutInflater.from(context).inflate(R.layout.face_main, null)
    private var showMoonAgeUntil: Long = 0
    private var fillWave = true
    private var circleType = CircleType.default
    private var latestDrawMode: DrawMode = DrawMode.INTERACTIVE
    private var zonedDateTimeOnStartedAmbient: ZonedDateTime? = null

    override suspend fun init() {
        super.init()

        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                fillWave = (userStyle[NocturneFaceService.fillWaveSetting] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption).value
                circleType = CircleType.fromOrdinal(
                    (userStyle[NocturneFaceService.circleTypeSetting] as UserStyleSetting.LongRangeUserStyleSetting.LongRangeOption).value.toInt()
                )
            }
        }
    }

    override suspend fun createSharedAssets(): SharedAssets = object : SharedAssets {

        override fun onDestroy() = Unit
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: SharedAssets) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        if (isAmbient && latestDrawMode != renderParameters.drawMode) {
            zonedDateTimeOnStartedAmbient = zonedDateTime
        }
        val moonAge = zonedDateTime.moonAge()

        canvas.drawColor(getBackgroundColor(isAmbient))

        //        getBackground(bounds)?.let {
        //            canvas.drawBitmap(it, 0f, 0f, Paint())
        //        }

        val width = bounds.height().toFloat()
        val height = bounds.height().toFloat()

        drawWave(canvas, if (isAmbient) zonedDateTimeOnStartedAmbient ?: zonedDateTime else zonedDateTime, width, height, moonAge, isAmbient)

        // CanvasComplicationDrawable already obeys rendererParameters.
        drawComplications(canvas, zonedDateTime)

        setDataToLayout(canvas, zonedDateTime, moonAge, isAmbient)

        val longerSideLength = max(width, height)
        val secondF: Float = zonedDateTime.toInstant().toEpochMilli() % 60000 * 0.001f

        drawCircle(canvas, zonedDateTime, secondF, longerSideLength, isAmbient)

        latestDrawMode = renderParameters.drawMode
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: SharedAssets) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        complicationSlotsManager.complicationSlots.forEach { (_, complication) ->
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawWave(canvas: Canvas, zonedDateTime: ZonedDateTime, width: Float, height: Float, moonAge: Double, isAmbient: Boolean) {
        val phase = zonedDateTime.toInstant().toEpochMilli() % 5600 * PI / 2800
        val path = Path()
        val paint = Paint().apply {
            style = if (fillWave) Paint.Style.FILL else Paint.Style.STROKE
            color = ContextCompat.getColor(context, if (isAmbient) R.color.waveAmbient else R.color.wave)
            isAntiAlias = true
            strokeWidth = if (isAmbient) 3f else 2f
        }

        path.moveTo(0f, (0.22f + abs(14 - (moonAge)).toFloat() * 0.65f / 14) * height + sin(-phase).toFloat() * 9f)
        val counts = (width / 10).toInt()
        repeat(counts) {
            val t = (it + 1).toFloat() / counts
            path.lineTo(
                t * width,
                (0.22f + abs(14 - (moonAge)).toFloat() * 0.65f / 14) * height + sin(t * 18 - phase).toFloat() * 9f
            )
        }
        if (fillWave) {
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
        }
        canvas.drawPath(path, paint)
    }

    //    private fun getBackground(bounds: Rect): Bitmap? =
    //        sharedPreferences.getString(PREFERENCE_KEY_BACKGROUND_IMAGE, null)
    //            ?.toByteArray()
    //            ?.let {
    //                val source = BitmapFactory.decodeByteArray(it, 0, it.size)
    //                val scale =
    //                    (if (source.width < source.height) bounds.width() else bounds.height()).toFloat() / min(source.width, source.height)
    //
    //                if (scale == 1f) {
    //                    source
    //                } else {
    //                    Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), false).apply {
    //                        source.recycle()
    //                    }
    //                }
    //            }

    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        complicationSlotsManager.complicationSlots.forEach { (_, complication) ->
            //            Timber.d("ngeck complication slot id: ${complication.id}, enabled: ${complication.enabled}, type: ${complication.complicationData.value.type}")
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun setDataToLayout(canvas: Canvas, zonedDateTime: ZonedDateTime, moonAge: Double, isAmbient: Boolean) {
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
            layout.apply {
                measure(spec.width, spec.height)
                layout(0, 0, measuredWidth, measuredHeight)
                findViewById<TextView>(R.id.time_primary).text = zonedDateTime.getPrimaryTimeString()
                findViewById<TextView>(R.id.time_secondary).apply {
                    if (isAmbient) {
                        visibility = View.GONE
                    } else {
                        visibility = View.VISIBLE
                        text = zonedDateTime.getTimeSecondString()
                    }
                }
                findViewById<TextView>(R.id.date).text = zonedDateTime.getDateString()
                findViewById<TextView>(R.id.moon_age).apply {
                    text = context.getString(R.string.message_moonage, moonAge)
                    visibility = if (showMoonAgeUntil > System.currentTimeMillis()) View.VISIBLE else View.GONE
                }
                draw(canvas)
            }
        }
    }

    private fun drawCircle(canvas: Canvas, zonedDateTime: ZonedDateTime, second: Float, longerSideLength: Float, isAmbient: Boolean) {
        if (isAmbient.not()) {
            val circleRect = RectF(
                17f,
                17f,
                longerSideLength - 17f,
                longerSideLength - 17f
            )
            val paint = Paint().apply {
                strokeWidth = 8f
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
                color = ContextCompat.getColor(context, R.color.circle)
                isAntiAlias = true
            }
            val hour = zonedDateTime.hour
            val minute = zonedDateTime.minute

            when (circleType) {
                CircleType.SECOND -> {
                    val isOdd = minute % 2 == 1
                    if (isOdd && second < FRAME_PERIOD_MS_DEFAULT * 0.001) {
                        canvas.drawCircle(circleRect.centerX(), circleRect.centerY(), circleRect.width() / 2, paint)
                    } else {
                        var startAngle = -90f
                        var sweepAngle = second * 360 / 60

                        if (isOdd) {
                            sweepAngle = 360f - sweepAngle
                            startAngle = -90f - sweepAngle
                        }

                        canvas.drawArc(circleRect, startAngle, sweepAngle, false, paint)
                    }
                }
                CircleType.MINUTE -> {
                    val isOdd = hour % 2 == 1
                    var startAngle = -90f
                    var sweepAngle = (minute + second / 60) * 360 / 60

                    if (isOdd) {
                        sweepAngle = 360f - sweepAngle
                        startAngle = -90f - sweepAngle
                    }

                    canvas.drawArc(circleRect, startAngle, sweepAngle, false, paint)
                }
                CircleType.HOUR -> {
                    val isOdd = hour / 12 == 1
                    var startAngle = -90f
                    var sweepAngle = (hour % 12 + minute / 60f + second / 3600) * 360 / 12

                    if (isOdd) {
                        sweepAngle = 360f - sweepAngle
                        startAngle = -90f - sweepAngle
                    }

                    canvas.drawArc(circleRect, startAngle, sweepAngle, false, paint)
                }
            }
        }
    }

    fun showMoonAge() {
        showMoonAgeUntil = System.currentTimeMillis() + 3000
    }

    private fun ZonedDateTime.getDateString(): String =
        "%04d-%02d-%02d %s".format(year, monthValue, dayOfMonth, getDayString())

    private fun ZonedDateTime.getPrimaryTimeString(): String =
        "%02d:%02d".format(hour, minute)

    private fun ZonedDateTime.getTimeSecondString(): String =
        "%02d".format(second)

    private fun ZonedDateTime.getDayString(): String? =
        when (dayOfWeek) {
            DayOfWeek.MONDAY -> context.getString(R.string.day_mon)
            DayOfWeek.TUESDAY -> context.getString(R.string.day_tue)
            DayOfWeek.WEDNESDAY -> context.getString(R.string.day_wed)
            DayOfWeek.THURSDAY -> context.getString(R.string.day_thu)
            DayOfWeek.FRIDAY -> context.getString(R.string.day_fri)
            DayOfWeek.SATURDAY -> context.getString(R.string.day_sat)
            DayOfWeek.SUNDAY -> context.getString(R.string.day_sun)
            else -> null
        }

    private fun getBackgroundColor(isAmbient: Boolean): Int =
        if (isAmbient) Color.BLACK
        else ContextCompat.getColor(context, R.color.background)
}
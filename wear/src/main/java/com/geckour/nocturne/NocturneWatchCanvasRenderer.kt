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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import timber.log.Timber
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
    currentUserStyleRepository: CurrentUserStyleRepository,
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

    override suspend fun createSharedAssets(): SharedAssets = object : SharedAssets {

        override fun onDestroy() = Unit
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: SharedAssets) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val moonAge = zonedDateTime.moonAge()

        // CanvasComplicationDrawable already obeys rendererParameters.
        drawComplications(canvas, zonedDateTime)

        canvas.drawColor(getBackgroundColor(isAmbient))

        val width = bounds.height().toFloat()
        val height = bounds.height().toFloat()

        if (isAmbient.not()) {
            val phase = zonedDateTime.toInstant().toEpochMilli() % 5600 * PI / 2800
            val path = Path()

            path.moveTo(0f, height)
            val counts = (width / 10).toInt()
            repeat(counts + 1) {
                val t = it.toFloat() / counts
                path.lineTo(
                    t * width,
                    (0.22f + abs(14 - (moonAge)).toFloat() * 0.65f / 14) * height + sin(t * 18 - phase).toFloat() * 9f
                )
            }
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()

            val paint = Paint().apply {
                strokeWidth = 1f
                style = Paint.Style.FILL
                color = ContextCompat.getColor(context, R.color.wave)
                isAntiAlias = true
            }
            canvas.drawPath(path, paint)
        }

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
                findViewById<ImageView>(R.id.wave).also {
                    val scale = measuredWidth.toFloat() / measuredHeight
                    it.scaleX = scale
                    it.scaleY = scale
                    it.translationY = (0.22f + abs(14 - (moonAge)).toFloat() * 0.65f / 14) * it.measuredHeight - 30f
                }
                findViewById<View>(R.id.wave).visibility = if (isAmbient) View.VISIBLE else View.GONE
                findViewById<TextView>(R.id.moon_age).apply {
                    text = context.getString(R.string.message_moonage, moonAge)
                    visibility = if (showMoonAgeUntil > System.currentTimeMillis()) View.VISIBLE else View.GONE
                }
                draw(canvas)
            }
        }

        if (isAmbient.not()) {
            val longerSideLength = max(width, height)
            val circleRect = RectF(
                longerSideLength * 0.04f,
                longerSideLength * 0.04f,
                longerSideLength * 0.96f,
                longerSideLength * 0.96f
            )
            val paint = Paint().apply {
                strokeWidth = 8f
                style = Paint.Style.STROKE
                color = ContextCompat.getColor(context, R.color.circle)
                isAntiAlias = true
            }
            val minute = zonedDateTime.minute
            val milli = zonedDateTime.toInstant().toEpochMilli()
            val secondF: Float = milli % 60000 * 0.001f
            val isOdd = minute % 2 == 1

            if (isOdd && secondF < FRAME_PERIOD_MS_DEFAULT * 0.001) {
                canvas.drawCircle(circleRect.centerX(), circleRect.centerY(), circleRect.width() / 2, paint)
            } else {
                var startAngle = -90f
                var sweepAngle = secondF * 360 / 60

                if (isOdd) {
                    sweepAngle = 360f - sweepAngle
                    startAngle = -90f - sweepAngle
                }

                canvas.drawArc(circleRect, startAngle, sweepAngle, false, paint)
            }
        }
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: SharedAssets) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        complicationSlotsManager.complicationSlots.forEach { (_, complication) ->
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    fun showMoonAge() {
        showMoonAgeUntil = System.currentTimeMillis() + 3000
    }

    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        complicationSlotsManager.complicationSlots.forEach { (_, complication) ->
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun ZonedDateTime.getDateString(): String =
        "%02d/%02d/%02d(%s)".format(year % 100, monthValue, dayOfMonth, getDayString())

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
package com.geckour.nocturne

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Size
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class NocturneFaceService : CanvasWatchFaceService() {

    companion object {
        /**
         * Updates rate in milliseconds for interactive mode. We update once a second since seconds
         * are displayed in interactive mode.
         */
        private const val INTERACTIVE_UPDATE_RATE_MS = 50

        /**
         * Handler message id for updating the time periodically in interactive mode.
         */
        private const val MSG_UPDATE_TIME = 0

        private const val PATH_ALARM_TIME = "/path_alarm_time"
        private const val KEY_ALARM_TIME = "value"
    }

    private val info = Info(false, Calendar.getInstance(), null)

    override fun onCreateEngine(): Engine = Engine()

    private class EngineHandler(reference: NocturneFaceService.Engine) : Handler() {
        private val weakReference: WeakReference<NocturneFaceService.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = weakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private var timeZoneReceiverRegistered = false

        private var burnInProtection: Boolean = false

        private val updateTimeHandler: Handler = EngineHandler(this)

        private val timeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                info.now.timeZone = TimeZone.getDefault()
                info.nextAlarmTime?.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        private lateinit var layout: View

        private lateinit var spec: Size

        private val onDataChanged: (DataEventBuffer) -> Unit = {
            it.forEach {
                when (it.type) {
                    DataEvent.TYPE_CHANGED -> {
                        if (it.dataItem.uri.path.compareTo(PATH_ALARM_TIME) == 0) {
                            val dataMap = DataMapItem.fromDataItem(it.dataItem).dataMap

                            val alarmTime = dataMap.getLong(KEY_ALARM_TIME)
                            try {
                                info.nextAlarmTime =
                                        Calendar.getInstance().apply { timeInMillis = alarmTime }
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

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                    WatchFaceStyle.Builder(this@NocturneFaceService).apply {
                        if (Build.VERSION.SDK_INT < 24) {
                            setShowSystemUiTime(false)
                            setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                        }
                        setStatusBarGravity(Gravity.LEFT or Gravity.TOP)
                        setAcceptsTapEvents(true)
                    }.build())

            layout = getSystemService(LayoutInflater::class.java)
                    .inflate(R.layout.face_main, null)

            val displaySize = Point().apply {
                getSystemService(WindowManager::class.java).defaultDisplay
                        .getSize(this)
            }
            spec = Size(
                    View.MeasureSpec.makeMeasureSpec(displaySize.x, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(displaySize.y, View.MeasureSpec.EXACTLY)
            )
        }

        override fun onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)

            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)

            burnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()

            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)

            info.isAmbient = inAmbientMode
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }

                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }

                WatchFaceService.TAP_TYPE_TAP ->
                    Toast.makeText(applicationContext,
                            getString(R.string.message_moonage, info.now.moonAge()),
                            Toast.LENGTH_SHORT
                    ).show()
            }

            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            info.now.timeInMillis = System.currentTimeMillis()
            layout.apply {
                measure(spec.width, spec.height)
                layout(0, 0, measuredWidth, measuredHeight)

                val alarmVisibility = if (info.nextAlarmTime == null) View.GONE else View.VISIBLE
                findViewById<View>(R.id.icon_alarm).visibility = alarmVisibility
                findViewById<View>(R.id.text_alarm).visibility = alarmVisibility
                findViewById<TextView>(R.id.text_alarm).text = info.nextAlarmTime?.getAlarmString()
                findViewById<TextView>(R.id.time_primary).text = info.now.getPrimaryTimeString()
                findViewById<TextView>(R.id.time_secondary).apply {
                    if (info.isAmbient) visibility = View.GONE
                    else {
                        visibility = View.VISIBLE
                        text = info.now.getTimeSecondString()
                    }
                }
                findViewById<TextView>(R.id.date).text = info.now.getDateString()
                findViewById<ImageView>(R.id.image_background).also {
                    val scale = measuredWidth.toFloat() / measuredHeight
                    it.scaleX = scale
                    it.scaleY = scale
                    it.translationY = it.measuredHeight *
                            (0.25f + abs(14 - (info.now.moonAge() ?: 14)).toFloat() * 0.65f / 14)
                    it.imageTintList =
                            if (info.isAmbient)
                                ColorStateList.valueOf(ContextCompat.getColor(applicationContext,
                                        R.color.backgroundImageAmbientTint))
                            else null
                }

                canvas.drawColor(info.getBackgroundColor())
                draw(canvas)

                if (info.isAmbient.not()) {
                    val longerSideLength = max(measuredWidth, measuredHeight)
                    val circleRect = RectF(
                            longerSideLength * 0.04f,
                            longerSideLength * 0.04f,
                            longerSideLength * 0.96f,
                            longerSideLength * 0.96f)
                    val paint = Paint().apply {
                        strokeWidth = 8f
                        style = Paint.Style.STROKE
                        color = ContextCompat.getColor(applicationContext, R.color.circle)
                        isAntiAlias = true
                    }

                    val minute = info.now.get(Calendar.MINUTE)
                    val second = info.now.get(Calendar.SECOND)
                    val milli = info.now.get(Calendar.MILLISECOND)
                    val secondF: Float = second + milli * 0.001f
                    val isOdd = minute % 2 == 1

                    if (isOdd && secondF < INTERACTIVE_UPDATE_RATE_MS * 0.001) {
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
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()

                // Update time zone in case it changed while we weren't visible.
                info.now.timeZone = TimeZone.getDefault()
                info.nextAlarmTime?.timeZone = TimeZone.getDefault()

                invalidate()
            } else {
                unregisterReceiver()
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        private fun registerReceiver() {
            Wearable.getDataClient(this@NocturneFaceService).addListener(onDataChanged)

            if (timeZoneReceiverRegistered) return

            timeZoneReceiverRegistered = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@NocturneFaceService.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            Wearable.getDataClient(this@NocturneFaceService).removeListener(onDataChanged)

            if (timeZoneReceiverRegistered.not()) return

            timeZoneReceiverRegistered = false
            this@NocturneFaceService.unregisterReceiver(timeZoneReceiver)
        }

        /**
         * Starts the [.updateTimeHandler] timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)

            if (shouldTimerBeRunning())
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
        }

        /**
         * Returns whether the [.updateTimeHandler] timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private fun shouldTimerBeRunning(): Boolean = isVisible && isInAmbientMode.not()

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()

            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        private fun Calendar.getAlarmString(): String =
                "%02d:%02d(%s)".format(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), getDayString())

        private fun Calendar.getPrimaryTimeString(): String =
                "%02d:%02d".format(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))

        private fun Calendar.getTimeSecondString(): String =
                "%02d".format(get(Calendar.SECOND))

        private fun Calendar.getDateString(): String =
                "%02d/%02d/%02d(%s)".format(get(Calendar.YEAR) % 100, get(Calendar.MONTH), get(Calendar.DATE), getDayString())

        private fun Info.getBackgroundColor(): Int =
                if (isAmbient) Color.BLACK
                else ContextCompat.getColor(applicationContext, R.color.background)

        private fun Calendar.getDayString(): String? =
                when (get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> applicationContext.getString(R.string.day_mon)
                    Calendar.TUESDAY -> applicationContext.getString(R.string.day_tue)
                    Calendar.WEDNESDAY -> applicationContext.getString(R.string.day_wed)
                    Calendar.THURSDAY -> applicationContext.getString(R.string.day_thu)
                    Calendar.FRIDAY -> applicationContext.getString(R.string.day_fri)
                    Calendar.SATURDAY -> applicationContext.getString(R.string.day_sat)
                    Calendar.SUNDAY -> applicationContext.getString(R.string.day_sun)
                    else -> null
                }
    }
}

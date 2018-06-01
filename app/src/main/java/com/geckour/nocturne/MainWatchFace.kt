package com.geckour.nocturne

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs

class MainWatchFace : CanvasWatchFaceService() {

    companion object {
        /**
         * Updates rate in milliseconds for interactive mode. We update once a second since seconds
         * are displayed in interactive mode.
         */
        private const val INTERACTIVE_UPDATE_RATE_MS = 100

        /**
         * Handler message id for updating the time periodically in interactive mode.
         */
        private const val MSG_UPDATE_TIME = 0
    }

    override fun onCreateEngine(): Engine = Engine()

    private class EngineHandler(reference: MainWatchFace.Engine) : Handler() {
        private val weakReference: WeakReference<MainWatchFace.Engine> = WeakReference(reference)

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

        private lateinit var calendar: Calendar

        private var timeZoneReceiverRegistered = false

        private var burnInProtection: Boolean = false

        private val updateTimeHandler: Handler = EngineHandler(this)

        private val timeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        private val info = Info(false, Calendar.getInstance(), null)

        private lateinit var layout: View

        private lateinit var spec: Size

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@MainWatchFace)
                    .setAcceptsTapEvents(true)
                    .build())

            layout = getSystemService(LayoutInflater::class.java).inflate(R.layout.face_main, null)

            val displaySize = Point().apply { getSystemService(WindowManager::class.java).defaultDisplay.getSize(this) }
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
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(applicationContext, R.string.message, Toast.LENGTH_SHORT)
                            .show()
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
                findViewById<ImageView>(R.id.image_background).apply {
                    translationY =
                            measuredHeight * (0.25f + abs(14 - (info.now.moonAge()
                            ?: 0)).toFloat() * 0.65f / 14)
                    imageTintList =
                            if (info.isAmbient)
                                ColorStateList.valueOf(ContextCompat.getColor(applicationContext,
                                        R.color.backgroundImageAmbientTint))
                            else null
                }

                canvas.drawColor(info.getBackgroundColor())
                draw(canvas)

                if (info.isAmbient.not()) {
                    val circleRect = RectF(measuredWidth * 0.04f, measuredHeight * 0.04f, measuredWidth * 0.96f, measuredHeight * 0.96f)
                    val paint = Paint().apply {
                        strokeWidth = 8f
                        style = Paint.Style.STROKE
                        color = ContextCompat.getColor(applicationContext, R.color.circle)
                        isAntiAlias = true
                    }

                    if (info.now.get(Calendar.SECOND) + info.now.get(Calendar.MILLISECOND) / 1000f < INTERACTIVE_UPDATE_RATE_MS * 0.001) {
                        canvas.drawCircle(circleRect.centerX(), circleRect.centerY(), circleRect.width() / 2, paint)
                    } else {
                        canvas.drawArc(
                                circleRect,
                                -90f,
                                (info.now.get(Calendar.SECOND) + info.now.get(Calendar.MILLISECOND) / 1000f) * 360f / 60,
                                false,
                                paint)
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
            if (timeZoneReceiverRegistered) return

            timeZoneReceiverRegistered = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MainWatchFace.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (timeZoneReceiverRegistered.not()) return

            timeZoneReceiverRegistered = false
            this@MainWatchFace.unregisterReceiver(timeZoneReceiver)
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
                " %02d:%02d (%s)".format(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), Info.getDayString(get(Calendar.DAY_OF_WEEK)))

        private fun Calendar.getPrimaryTimeString(): String =
                "%02d:%02d".format(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))

        private fun Calendar.getTimeSecondString(): String =
                "%02d".format(get(Calendar.SECOND))

        private fun Calendar.getDateString(): String =
                "%02d/%02d/%02d (%s)".format(get(Calendar.YEAR) % 100, get(Calendar.MONTH), get(Calendar.DATE), Info.getDayString(get(Calendar.DAY_OF_WEEK)))

        private fun Info.getBackgroundColor(): Int =
                if (isAmbient) Color.BLACK
                else ContextCompat.getColor(applicationContext, R.color.background)
    }
}

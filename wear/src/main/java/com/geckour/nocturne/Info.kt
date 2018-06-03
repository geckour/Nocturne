package com.geckour.nocturne

import android.content.Context
import java.util.*

data class Info(
        var isAmbient: Boolean,
        val now: Calendar,
        var nextAlarmTime: Calendar?
) {
    companion object {
        @JvmStatic
        fun getDayString(context: Context, day: Int): String? =
                when (day) {
                    Calendar.MONDAY -> context.getString(R.string.day_mon)
                    Calendar.TUESDAY -> context.getString(R.string.day_tue)
                    Calendar.WEDNESDAY -> context.getString(R.string.day_wed)
                    Calendar.THURSDAY -> context.getString(R.string.day_thu)
                    Calendar.FRIDAY -> context.getString(R.string.day_fri)
                    Calendar.SATURDAY -> context.getString(R.string.day_sat)
                    Calendar.SUNDAY -> context.getString(R.string.day_sun)
                    else -> null
                }
    }
}
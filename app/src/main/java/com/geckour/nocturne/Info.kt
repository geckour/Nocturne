package com.geckour.nocturne

import java.util.*

data class Info(
        var isAmbient: Boolean,
        val now: Calendar,
        var nextAlarmTime: Calendar?
) {
    companion object {
        @JvmStatic
        fun getDayString(day: Int): String? =
                when (day) {
                    Calendar.MONDAY -> "月"
                    Calendar.TUESDAY -> "火"
                    Calendar.WEDNESDAY -> "水"
                    Calendar.THURSDAY -> "木"
                    Calendar.FRIDAY -> "金"
                    Calendar.SATURDAY -> "土"
                    Calendar.SUNDAY -> "日"
                    else -> null
                }
    }
}
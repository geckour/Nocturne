package com.geckour.nocturne

import java.util.*

data class Info(
        var isAmbient: Boolean,
        val now: Calendar,
        var nextAlarmTime: Calendar?
)
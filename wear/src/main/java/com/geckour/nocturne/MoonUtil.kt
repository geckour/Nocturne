package com.geckour.nocturne

import com.mhuss.AstroLib.DateOps
import com.mhuss.AstroLib.LunarCalc
import java.time.ZonedDateTime
import java.util.*

val ZonedDateTime.moonAge: Float
    get() = LunarCalc.ageOfMoonInDays(DateOps.calendarToDoubleDay(GregorianCalendar.from(this))).toFloat() +
            this.minute.toFloat() / 1440 +
            this.second.toFloat() / 86400
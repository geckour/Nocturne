package com.geckour.nocturne

import java.util.*

fun Calendar.moonAge(): Int? =
        c(get(Calendar.MONTH))?.let { (((get(Calendar.YEAR) % 100) - 11) % 19 * 11 + it + get(Calendar.DATE)) % 30 }

private fun c(month: Int): Int? =
        when (month) {
            1 -> 0
            2 -> 2
            3 -> 0
            4 -> 2
            5 -> 2
            6 -> 4
            7 -> 5
            8 -> 6
            9 -> 7
            10 -> 8
            11 -> 9
            12 -> 10
            else -> null
        }

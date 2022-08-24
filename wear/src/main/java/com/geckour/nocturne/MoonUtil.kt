package com.geckour.nocturne

import java.time.ZonedDateTime
import java.util.*
import kotlin.math.cos
import kotlin.math.floor

fun ZonedDateTime.moonAge(): Double {
    val t = getJapaneseDay(year, monthValue, dayOfMonth.toDouble()) - getJapaneseDay(2000, 1, 1.5)
    var dm = floor((t.toDouble().toMoonLong() - t.toDouble().toSunLong()).let { if (it < 0.0) it + 360.0 else it } / 13.5) - 1
    var dStep = 1.0
    var a0: Double
    var a1 = 999.0
    while (true) {
        a0 = ((t - dm).toMoonLong() - (t - dm).toSunLong()).let { if (it < 0.0) it + 360.0 else it }
        if (a0 > a1) {
            if (dStep < 0.1) break
            dm -= dStep
            dStep /= 2.0
        } else {
            a1 = a0
        }
        dm += dStep
    }
    a0 -= 360.0
    dm += dStep * a0 / (a1 - a0) // 朔を越えたら直線近似で朔の時刻を求める

    return dm
}

private fun Double.toMoonLong(): Double {
    val t = this / 36525.0
    var ans = 0.0
    (62 downTo 1).forEach {
        val angle = (mlb[it] * t + mlc[it]) * d2r
        ans += mla[it] * cos(angle)
    }
    val angle = (mlb[0] * t + mlc[0] * d2r)
    ans += mla[0] * t * cos(angle)
    ans -= floor(ans / 360.0) * 360.0
    return ans
}

private fun Double.toSunLong(): Double {
    val dans = -0.0057 + 0.0048 * cos((1934 * this / 36525.0 + 145) * d2r)
    var ans = this.toSunMLong() + dans
    while (ans < 0.0) ans += 360.0
    while (ans >= 360.0) ans -= 360.0
    return ans
}

private fun Double.toSunMLong(): Double {
    val t = this / 36525.0
    var ans = 0.0
    (17 downTo 0).forEach {
        val angle = (slb[it] * this + slc[it]) * d2r
        ans += if ((it == 0) || (it == 4)) sla[it] * t * cos(angle)
        else sla[it] * cos(angle)
    }
    ans -= floor(ans / 360.0) * 360.0

    return ans
}

private fun getJapaneseDay(year: Int, month: Int, date: Double): Int {
    val daysInMonthList = listOf(31, if (year.isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val yym1 = year - 1

    return (1721422 + floor(365.25 * yym1 + 0.1) + daysInMonthList.subList(0, month - 1).sum() + date).let {
        val result = it

        if (result >= 2299160) result - 10
        if (yym1 >= 1600)
            result - floor((yym1 - 1600 + 0.1) / 100) + floor((yym1 - 1600 + 0.1) / 400)

        return@let result.toInt()
    }
}

private val mla = listOf(
    481267.8809,
    218.3162,
    6.2888,
    1.2740,
    0.6583,
    0.2136,
    0.1851,
    0.1144,
    0.0588,
    0.0571,
    0.0533,
    0.0458,
    0.0409,
    0.0347,
    0.0304,
    0.0154,
    0.0125,
    0.0110,
    0.0107,
    0.0100,
    0.0085,
    0.0079,
    0.0068,
    0.0052,
    0.0050,
    0.0040,
    0.0040,
    0.0040,
    0.0038,
    0.0037,
    0.0028,
    0.0027,
    0.0026,
    0.0024,
    0.0023,
    0.0022,
    0.0021,
    0.0021,
    0.0021,
    0.0018,
    0.0016,
    0.0012,
    0.0011,
    0.0009,
    0.0008,
    0.0007,
    0.0007,
    0.0007,
    0.0007,
    0.0006,
    0.0006,
    0.0005,
    0.0005,
    0.0005,
    0.0004,
    0.0004,
    0.0003,
    0.0003,
    0.0003,
    0.0003,
    0.0003,
    0.0003,
    0.0003
)
private val mlb = listOf(
    0.0,
    0.0,
    477198.868,
    413335.35,
    890534.22,
    954397.74,
    35999.05,
    966404.0,
    63863.5,
    377336.3,
    1367733.1,
    854535.2,
    441199.8,
    445267.1,
    513197.9,
    75870.0,
    1443603.0,
    489205.0,
    1303870.0,
    1431597.0,
    826671.0,
    449334.0,
    926533.0,
    31932.0,
    481266.0,
    1331734.0,
    1844932.0,
    133.0,
    1781068.0,
    541062.0,
    1934.0,
    918399.0,
    1379739.0,
    99863.0,
    922466.0,
    818536.0,
    990397.0,
    71998.0,
    341337.0,
    401329.0,
    1856938.0,
    1267871.0,
    1920802.0,
    858602.0,
    1403732.0,
    790672.0,
    405201.0,
    485333.0,
    27864.0,
    111869.0,
    2258267.0,
    1908795.0,
    1745069.0,
    509131.0,
    39871.0,
    12006.0,
    958465.0,
    381404.0,
    349472.0,
    1808933.0,
    549197.0,
    4067.0,
    2322131.0
)
private val mlc = listOf(
    0.0,
    0.0,
    44.963,
    10.74,
    145.7,
    179.93,
    87.53,
    276.5,
    124.2,
    13.2,
    280.7,
    148.2,
    47.4,
    27.9,
    222.5,
    41.0,
    52.0,
    142.0,
    246.0,
    315.0,
    111.0,
    188.0,
    323.0,
    107.0,
    205.0,
    283.0,
    56.0,
    29.0,
    21.0,
    259.0,
    145.0,
    182.0,
    17.0,
    122.0,
    163.0,
    151.0,
    357.0,
    85.0,
    16.0,
    274.0,
    152.0,
    249.0,
    186.0,
    129.0,
    98.0,
    114.0,
    50.0,
    186.0,
    127.0,
    38.0,
    156.0,
    90.0,
    24.0,
    242.0,
    223.0,
    187.0,
    340.0,
    354.0,
    337.0,
    58.0,
    220.0,
    70.0,
    191.0
)
private val sla = listOf(
    36000.7695,
    280.4659,
    1.9147,
    0.0200,
    -0.0048,
    0.0020,
    0.0018,
    0.0018,
    0.0015,
    0.0013,
    0.0007,
    0.0007,
    0.0007,
    0.0006,
    0.0005,
    0.0005,
    0.0004,
    0.0004
)
private val slb = listOf(
    0.0,
    0.0,
    35999.05,
    71998.1,
    35999.0,
    32964.0,
    19.0,
    445267.0,
    45038.0,
    22519.0,
    65929.0,
    3035.0,
    9038.0,
    33718.0,
    155.0,
    2281.0,
    29930.0,
    31557.0
)
private val slc = listOf(0.0, 0.0, 267.52, 265.1, 268.0, 158.0, 159.0, 208.0, 254.0, 352.0, 45.0, 110.0, 64.0, 316.0, 118.0, 221.0, 48.0, 161.0)
private const val d2r = Math.PI / 180.0

private val Int.isLeapYear: Boolean
    get() =
        Calendar.getInstance().apply { set(Calendar.YEAR, this@isLeapYear) }
            .getActualMaximum(Calendar.DAY_OF_YEAR) == 366

package com.geckour.nocturne

enum class CircleType(val displayName: String) {
    SECOND("Sec"),
    MINUTE("Min"),
    HOUR("Hour");

    companion object {

        val default = SECOND

        fun fromOrdinal(ordinal: Int, default: CircleType = Companion.default): CircleType = values().firstOrNull { it.ordinal == ordinal } ?: default
    }
}
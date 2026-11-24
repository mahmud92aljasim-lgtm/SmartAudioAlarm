package com.example.smartaudioalarm

import android.net.Uri

data class AlarmItem(
    val id: Int,
    var hourOfDay: Int,
    var minute: Int,
    var audioUri: Uri,
    var audioTitle: String,
    var label: String = "",
    var isEnabled: Boolean = true
) {
    fun formattedTime12h(): String {
        val period = if (hourOfDay >= 12) "م (PM)" else "ص (AM)"
        val hour12 = when {
            hourOfDay == 0 -> 12
            hourOfDay > 12 -> hourOfDay - 12
            else -> hourOfDay
        }
        return String.format("%02d:%02d %s", hour12, minute, period)
    }
}

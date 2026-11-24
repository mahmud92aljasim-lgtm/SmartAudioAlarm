package com.example.smartaudioalarm

data class AppSettings(
    val gradualVolume: Boolean = true,
    val fadeDurationSec: Int = 15,
    val targetVolumePercent: Int = 80,
    val autoStopMinutes: Int = 5,
    val snoozeMinutes: Int = 5,
    val vibrateOnAlarm: Boolean = true
)

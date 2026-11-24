package com.example.smartaudioalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val audioUri = intent.getStringExtra("AUDIO_URI") ?: return
        val audioTitle = intent.getStringExtra("AUDIO_TITLE") ?: "مقطع صوتي"
        val label = intent.getStringExtra("ALARM_LABEL") ?: ""

        val serviceIntent = Intent(context, AudioAlarmService::class.java).apply {
            putExtra("AUDIO_URI", audioUri)
            putExtra("AUDIO_TITLE", audioTitle)
            putExtra("ALARM_LABEL", label)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

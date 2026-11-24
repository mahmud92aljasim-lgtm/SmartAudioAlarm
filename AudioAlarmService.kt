package com.example.smartaudioalarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat

class AudioAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentUri: String? = null
    private var currentTitle: String = "مقطع صوتي"

    companion object {
        const val ACTION_STOP = "ACTION_STOP_AUDIO_ALARM"
        const val ACTION_SNOOZE = "ACTION_SNOOZE_AUDIO_ALARM"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = AlarmStorage.loadSettings(this)

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val uri = currentUri ?: intent.getStringExtra("AUDIO_URI")
                val title = intent.getStringExtra("AUDIO_TITLE") ?: currentTitle
                if (uri != null) {
                    AlarmScheduler.scheduleSnooze(this, uri, title, settings.snoozeMinutes)
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val uriString = intent?.getStringExtra("AUDIO_URI")
        val title = intent?.getStringExtra("AUDIO_TITLE") ?: "تشغيل صوت مجدول"
        val label = intent?.getStringExtra("ALARM_LABEL").orEmpty()

        currentUri = uriString
        currentTitle = title

        startAsForeground(if (label.isNotBlank()) "$label - $title" else title, uriString, settings.snoozeMinutes)

        if (uriString != null) {
            playAudioWithSettings(Uri.parse(uriString), settings)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playAudioWithSettings(uri: Uri, settings: AppSettings) {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val desiredVol = (maxVol * (settings.targetVolumePercent / 100f)).toInt().coerceIn(1, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desiredVol, 0)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, uri)
            prepare()
            setOnCompletionListener { stopSelf() }

            if (settings.gradualVolume) {
                setVolume(0.05f, 0.05f)
                start()
                startFadeIn(settings.fadeDurationSec)
            } else {
                setVolume(1.0f, 1.0f)
                start()
            }
        }

        if (settings.autoStopMinutes > 0) {
            handler.postDelayed({ stopSelf() }, settings.autoStopMinutes * 60_000L)
        }
    }

    private fun startFadeIn(durationSec: Int) {
        val steps = 20
        val intervalMs = (durationSec * 1000L) / steps
        var currentStep = 1

        val runnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return
                if (!player.isPlaying) return
                val vol = (currentStep.toFloat() / steps).coerceIn(0.05f, 1.0f)
                player.setVolume(vol, vol)
                if (currentStep < steps) {
                    currentStep++
                    handler.postDelayed(this, intervalMs)
                }
            }
        }
        handler.postDelayed(runnable, intervalMs)
    }

    private fun startAsForeground(displayTitle: String, uriString: String?, snoozeMin: Int) {
        val channelId = "smart_audio_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "منبه الصوت الذكي",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AudioAlarmService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AudioAlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("AUDIO_URI", uriString)
            putExtra("AUDIO_TITLE", currentTitle)
        }
        val snoozePending = PendingIntent.getService(
            this, 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("يعمل الآن: $displayTitle")
            .setContentText("يمكنك الإيقاف أو الغفوة لمدة $snoozeMin دقائق")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPending)
            .addAction(android.R.drawable.ic_popup_reminder, "غفوة ($snoozeMin د)", snoozePending)
            .setOngoing(true)
            .build()

        startForeground(2001, notification)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.smartaudioalarm

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object AlarmStorage {

    private const val PREF_NAME = "smart_audio_alarm_prefs"
    private const val KEY_ALARMS = "saved_alarms"
    private const val KEY_NEXT_ID = "next_alarm_id"

    fun getNextId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        return id
    }

    fun saveAlarms(context: Context, alarms: List<AlarmItem>) {
        val jsonArray = JSONArray()
        alarms.forEach { alarm ->
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("hourOfDay", alarm.hourOfDay)
                put("minute", alarm.minute)
                put("audioUri", alarm.audioUri.toString())
                put("audioTitle", alarm.audioTitle)
                put("label", alarm.label)
                put("isEnabled", alarm.isEnabled)
            }
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARMS, jsonArray.toString())
            .apply()
    }

    fun loadAlarms(context: Context): MutableList<AlarmItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ALARMS, null) ?: return mutableListOf()
        val list = mutableListOf<AlarmItem>()
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                AlarmItem(
                    id = obj.getInt("id"),
                    hourOfDay = obj.getInt("hourOfDay"),
                    minute = obj.getInt("minute"),
                    audioUri = Uri.parse(obj.getString("audioUri")),
                    audioTitle = obj.getString("audioTitle"),
                    label = obj.optString("label", ""),
                    isEnabled = obj.getBoolean("isEnabled")
                )
            )
        }
        return list
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean("gradualVolume", settings.gradualVolume)
            putInt("fadeDurationSec", settings.fadeDurationSec)
            putInt("targetVolumePercent", settings.targetVolumePercent)
            putInt("autoStopMinutes", settings.autoStopMinutes)
            putInt("snoozeMinutes", settings.snoozeMinutes)
            putBoolean("vibrateOnAlarm", settings.vibrateOnAlarm)
            apply()
        }
    }

    fun loadSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return AppSettings(
            gradualVolume = prefs.getBoolean("gradualVolume", true),
            fadeDurationSec = prefs.getInt("fadeDurationSec", 15),
            targetVolumePercent = prefs.getInt("targetVolumePercent", 80),
            autoStopMinutes = prefs.getInt("autoStopMinutes", 5),
            snoozeMinutes = prefs.getInt("snoozeMinutes", 5),
            vibrateOnAlarm = prefs.getBoolean("vibrateOnAlarm", true)
        )
    }
}

package com.example.smartaudioalarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class MainActivity : AppCompatActivity() {

    private val alarms = mutableListOf<AlarmItem>()
    private lateinit var adapter: AlarmAdapter
    private lateinit var tvEmptyState: TextView

    private var pendingHourOfDay: Int = 7
    private var pendingMinute: Int = 0
    private var editingAlarmIndex: Int? = null

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { }

        val title = queryDisplayName(uri) ?: "ملف صوتي"
        promptLabelAndSave(pendingHourOfDay, pendingMinute, uri, title)
    }

        private val clockHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var tvMainTime: TextView
    private lateinit var tvSeconds: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvActiveCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val errorMsg = android.util.Log.getStackTraceString(throwable)
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("سبب توقف التطبيق")
                    .setMessage(errorMsg.take(600))
                    .setPositiveButton("نسخ الخطأ") { _, _ ->
                        val cb = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cb.setPrimaryClip(android.content.ClipData.newPlainText("err", errorMsg))
                    }
                    .show()
            }
            Thread.sleep(4000)
                }
        setContentView(R.layout.activity_main)

        tvMainTime = findViewById(R.id.tvMainTime)
        tvSeconds = findViewById(R.id.tvSeconds)
        tvDate = findViewById(R.id.tvDate)
        tvActiveCount = findViewById(R.id.tvActiveCount)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        alarms.addAll(AlarmStorage.loadAlarms(this)) 
        val rvAlarms = findViewById<RecyclerView>(R.id.rvAlarms)
        adapter = AlarmAdapter(
            items = alarms,
            onToggle = { alarm, isEnabled ->
                AlarmStorage.saveAlarms(this, alarms)
                if (isEnabled) AlarmScheduler.schedule(this, alarm)
                else AlarmScheduler.cancel(this, alarm.id)
                updateEmptyState()
            },
            onEdit = { alarm, index ->
                editingAlarmIndex = index
                showTimePicker12Hour(alarm.hourOfDay, alarm.minute, alarm)
            },
            onDelete = { alarm, index ->
                confirmDeleteAlarm(alarm, index)
            }
        )

        rvAlarms.layoutManager = LinearLayoutManager(this)
        rvAlarms.adapter = adapter
        updateEmptyState()

        // زر الإضافة الدائري
        findViewById<View>(R.id.fabAddAlarm).setOnClickListener {
            editingAlarmIndex = null
            showTimePicker12Hour(7, 0, null)
        }

        // زر الإعدادات الدائري
        findViewById<View>(R.id.btnHudSettings).setOnClickListener {
            showSettingsDialog()
        }

        startLiveClock()
    }

    private fun startLiveClock() {
        val timeFmt = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH)
        val secFmt = java.text.SimpleDateFormat(":ss", java.util.Locale.ENGLISH)
        val dateFmt = java.text.SimpleDateFormat("EEE • dd MMM yyyy", java.util.Locale.ENGLISH)

        clockHandler.post(object : Runnable {
            override fun run() {
                val now = java.util.Date()
                tvMainTime.text = timeFmt.format(now)
                tvSeconds.text = secFmt.format(now)
                tvDate.text = dateFmt.format(now)
                clockHandler.postDelayed(this, 1000)
            }
        })
    }
    
private fun showTimePicker12Hour(initialHour: Int, initialMinute: Int, existingAlarm: AlarmItem?) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText(if (existingAlarm == null) "ضبط وقت جديد (12 ساعة)" else "تعديل الوقت")
            .build()

        picker.addOnPositiveButtonClickListener {
            pendingHourOfDay = picker.hour
            pendingMinute = picker.minute

            if (existingAlarm != null) {
                AlertDialog.Builder(this)
                    .setTitle("تعديل الصوت")
                    .setMessage("الصوت الحالي: ${existingAlarm.audioTitle}")
                    .setPositiveButton("اختيار صوت جديد") { _, _ ->
                        pickAudioLauncher.launch(arrayOf("audio/*"))
                    }
                    .setNegativeButton("الإبقاء على نفس الصوت") { _, _ ->
                        promptLabelAndSave(
                            pendingHourOfDay,
                            pendingMinute,
                            existingAlarm.audioUri,
                            existingAlarm.audioTitle,
                            existingAlarm.label
                        )
                    }
                    .show()
            } else {
                pickAudioLauncher.launch(arrayOf("audio/*"))
            }
        }

        picker.show(supportFragmentManager, "TIME_PICKER_12H")
    }

    private fun promptLabelAndSave(
        hour: Int,
        minute: Int,
        uri: Uri,
        title: String,
        defaultLabel: String = ""
    ) {
        val input = EditText(this).apply {
            hint = "تسمية اختيارية (مثلاً: أذان، موسيقى الصباح...)"
            setText(defaultLabel)
        }

        AlertDialog.Builder(this)
            .setTitle("تسمية المنبه")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val labelText = input.text.toString().trim()
                val editIdx = editingAlarmIndex

                if (editIdx != null && editIdx in alarms.indices) {
                    val updated = alarms[editIdx].apply {
                        hourOfDay = hour
                        this.minute = minute
                        audioUri = uri
                        audioTitle = title
                        label = labelText
                        isEnabled = true
                    }
                    adapter.notifyItemChanged(editIdx)
                    AlarmStorage.saveAlarms(this, alarms)
                    AlarmScheduler.schedule(this, updated)
                    Toast.makeText(this, "تم تحديث المنبه", Toast.LENGTH_SHORT).show()
                } else {
                    val newAlarm = AlarmItem(
                        id = AlarmStorage.getNextId(this),
                        hourOfDay = hour,
                        minute = minute,
                        audioUri = uri,
                        audioTitle = title,
                        label = labelText,
                        isEnabled = true
                    )
                    alarms.add(0, newAlarm)
                    adapter.notifyItemInserted(0)
                    AlarmStorage.saveAlarms(this, alarms)
                    AlarmScheduler.schedule(this, newAlarm)
                    Toast.makeText(this, "تم ضبط المنبه على ${newAlarm.formattedTime12h()}", Toast.LENGTH_SHORT).show()
                }
                updateEmptyState()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeleteAlarm(alarm: AlarmItem, index: Int) {
        AlertDialog.Builder(this)
            .setTitle("حذف المنبه")
            .setMessage("هل تريد حذف منبه ${alarm.formattedTime12h()}؟")
            .setPositiveButton("حذف") { _, _ ->
                AlarmScheduler.cancel(this, alarm.id)
                alarms.removeAt(index)
                adapter.notifyItemRemoved(index)
                AlarmStorage.saveAlarms(this, alarms)
                updateEmptyState()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showSettingsDialog() {
        val current = AlarmStorage.loadSettings(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val swGradual = SwitchMaterial(this).apply {
            text = "رفع الصوت تدريجياً (Fade-in)"
            isChecked = current.gradualVolume
        }

        val tvVol = TextView(this).apply {
            text = "مستوى الصوت المستهدف: ${current.targetVolumePercent}%"
            setPadding(0, 20, 0, 4)
        }
        val seekVol = SeekBar(this).apply {
            max = 100
            progress = current.targetVolumePercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                    tvVol.text = "مستوى الصوت المستهدف: ${p.coerceAtLeast(10)}%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }

        val tvAutoStop = TextView(this).apply {
            text = "إيقاف التشغيل تلقائياً بعد (بالدقائق - 0 لتشغيل كامل الملف):"
            setPadding(0, 20, 0, 4)
        }
        val etAutoStop = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(current.autoStopMinutes.toString())
        }

        val tvSnooze = TextView(this).apply {
            text = "مدة الغفوة Snooze (بالدقائق):"
            setPadding(0, 16, 0, 4)
        }
        val etSnooze = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(current.snoozeMinutes.toString())
        }

        val btnBattery = Button(this).apply {
            text = "استثناء التطبيق من توفير البطارية (مهم للعمل التلقائي)"
            setOnClickListener { requestIgnoreBatteryOptimizations() }
        }

        container.addView(swGradual)
        container.addView(tvVol)
        container.addView(seekVol)
        container.addView(tvAutoStop)
        container.addView(etAutoStop)
        container.addView(tvSnooze)
        container.addView(etSnooze)
        container.addView(btnBattery)

        AlertDialog.Builder(this)
            .setTitle("إعدادات التشغيل والراحة")
            .setView(container)
            .setPositiveButton("حفظ الإعدادات") { _, _ ->
                val updated = AppSettings(
                    gradualVolume = swGradual.isChecked,
                    targetVolumePercent = seekVol.progress.coerceAtLeast(10),
                    autoStopMinutes = etAutoStop.text.toString().toIntOrNull() ?: 5,
                    snoozeMinutes = (etSnooze.text.toString().toIntOrNull() ?: 5).coerceAtLeast(1)
                )
                AlarmStorage.saveSettings(this, updated)
                Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "التطبيق مستثنى بالفعل من قيود البطارية", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState() {
        tvEmptyState.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
        tvActiveCount.text = alarms.count { it.isEnabled }.toString()
        
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }
}

  

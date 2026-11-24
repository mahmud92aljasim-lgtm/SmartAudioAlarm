package com.example.smartaudioalarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class AlarmAdapter(
    private val items: MutableList<AlarmItem>,
    private val onToggle: (AlarmItem, Boolean) -> Unit,
    private val onEdit: (AlarmItem, Int) -> Unit,
    private val onDelete: (AlarmItem, Int) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvAlarmTime)
        val tvLabel: TextView = view.findViewById(R.id.tvAlarmLabel)
        val tvAudioName: TextView = view.findViewById(R.id.tvAudioName)
        val switchAlarm: SwitchMaterial = view.findViewById(R.id.switchAlarm)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditAlarm)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteAlarm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val item = items[position]
        holder.tvTime.text = item.formattedTime12h()
        holder.tvAudioName.text = "🎵 ${item.audioTitle}"
        holder.tvLabel.visibility = if (item.label.isNotBlank()) View.VISIBLE else View.GONE
        holder.tvLabel.text = item.label

        holder.switchAlarm.setOnCheckedChangeListener(null)
        holder.switchAlarm.isChecked = item.isEnabled
        holder.switchAlarm.setOnCheckedChangeListener { _, checked ->
            item.isEnabled = checked
            onToggle(item, checked)
        }

        holder.btnEdit.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onEdit(items[pos], pos)
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(items[pos], pos)
        }
    }

    override fun getItemCount(): Int = items.size
}

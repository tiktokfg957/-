package com.example.budgettracker

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import java.util.*

class SettingsFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var timePicker: TimePicker
    private lateinit var btnSave: Button
    private lateinit var tvStatus: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireActivity().getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

        timePicker = view.findViewById(R.id.timePickerDaily)
        btnSave = view.findViewById(R.id.btnSaveDailyReminder)
        tvStatus = view.findViewById(R.id.tvDailyReminderStatus)

        // Устанавливаем текущее время из настроек
        val savedHour = prefs.getInt("daily_reminder_hour", 20)
        val savedMinute = prefs.getInt("daily_reminder_minute", 0)
        timePicker.hour = savedHour
        timePicker.minute = savedMinute
        updateStatusText(savedHour, savedMinute)

        btnSave.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            prefs.edit().putInt("daily_reminder_hour", hour).apply()
            prefs.edit().putInt("daily_reminder_minute", minute).apply()
            updateStatusText(hour, minute)
            ReminderHelper.scheduleDailyReminder(requireContext())
            android.widget.Toast.makeText(requireContext(), "Время сохранено", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusText(hour: Int, minute: Int) {
        val timeStr = String.format("%02d:%02d", hour, minute)
        tvStatus.text = "Ежедневное напоминание установлено на $timeStr"
    }
}

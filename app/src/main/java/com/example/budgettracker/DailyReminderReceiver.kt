package com.example.budgettracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderHelper.showNotification(
            context,
            "Не забудьте внести траты",
            "Добавьте сегодняшние расходы в приложение, чтобы контролировать бюджет."
        )
        // Перезапускаем на следующий день
        ReminderHelper.scheduleDailyReminder(context)
    }
}

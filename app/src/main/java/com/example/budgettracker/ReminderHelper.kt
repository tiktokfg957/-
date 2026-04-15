package com.example.budgettracker

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderHelper {
    fun scheduleReminder(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS) // первый раз через день
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "budget_reminder",
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}

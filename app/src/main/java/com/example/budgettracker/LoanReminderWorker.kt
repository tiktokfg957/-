package com.example.budgettracker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.Calendar

class LoanReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("budget_data", Context.MODE_PRIVATE)
        val loansJson = prefs.getString("loans", "[]") ?: "[]"
        val jsonArray = JSONArray(loansJson)
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val name = obj.getString("name")
            val paymentDay = obj.getInt("paymentDay")
            val minPayment = obj.getDouble("minPayment")
            val remaining = obj.getDouble("remaining")

            if (remaining > 0) {
                // Проверяем, не наступил ли день платежа или за 3 дня до него
                val diff = paymentDay - currentDay
                if (diff == 3) {
                    ReminderHelper.showNotification(
                        applicationContext,
                        "Скоро платёж по кредиту: $name",
                        "Через 3 дня нужно внести ${String.format("%.2f", minPayment)} ₽. Остаток: ${String.format("%.2f", remaining)} ₽."
                    )
                } else if (diff == 0) {
                    ReminderHelper.showNotification(
                        applicationContext,
                        "Сегодня платёж по кредиту: $name",
                        "Сегодня последний день для внесения ${String.format("%.2f", minPayment)} ₽. Не забудьте оплатить!"
                    )
                } else if (diff == -1) {
                    // Пропущен платёж? Можно уведомить о просрочке
                    ReminderHelper.showNotification(
                        applicationContext,
                        "Просрочка платежа по кредиту: $name",
                        "Платёж за этот месяц ещё не внесён. Пожалуйста, оплатите ${String.format("%.2f", minPayment)} ₽."
                    )
                }
            }
        }
        return Result.success()
    }
}

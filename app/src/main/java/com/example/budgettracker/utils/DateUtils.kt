package com.example.budgettracker.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun timestampToString(timestamp: Long): String = format.format(Date(timestamp))

    fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }
}

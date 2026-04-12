package com.example.budgettracker

import android.app.Application
import com.example.budgettracker.data.database.AppDatabase
import com.example.budgettracker.data.repository.BudgetRepository

class BudgetApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { BudgetRepository(database) }
}

package com.example.budgettracker.data.repository

import com.example.budgettracker.data.database.AppDatabase
import com.example.budgettracker.data.model.Transaction
import com.example.budgettracker.data.model.ShopStat
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val db: AppDatabase) {

    fun getAllTransactions(): Flow<List<Transaction>> = db.transactionDao().getAllTransactions()
    suspend fun insertTransaction(transaction: Transaction) = db.transactionDao().insert(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = db.transactionDao().delete(transaction)

    suspend fun getTotalExpenseSince(startDate: Long): Double = db.transactionDao().getTotalExpenseSince(startDate) ?: 0.0
    suspend fun getShopStats(startDate: Long): List<ShopStat> = db.transactionDao().getShopStats(startDate)
}

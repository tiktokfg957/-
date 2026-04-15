package com.example.budgettracker.data.database

import androidx.room.*
import com.example.budgettracker.data.model.ShopStat
import com.example.budgettracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE date >= :startDate")
    suspend fun getTotalExpenseSince(startDate: Long): Double?

    @Query("SELECT shop, SUM(amount) as total FROM transactions WHERE date >= :startDate GROUP BY shop ORDER BY total DESC")
    suspend fun getShopStats(startDate: Long): List<ShopStat>
}

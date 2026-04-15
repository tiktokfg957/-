package com.example.budgettracker.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.example.budgettracker.data.database.ShopStat
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.utils.DateUtils

class StatsViewModel(private val repository: BudgetRepository) : ViewModel() {

    val shopStats: LiveData<List<ShopStat>> = liveData {
        emit(repository.getShopStats(DateUtils.getStartOfMonth()))
    }

    class StatsViewModelFactory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StatsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

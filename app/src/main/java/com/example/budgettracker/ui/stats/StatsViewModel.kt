package com.example.budgettracker.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.model.ShopStat
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.utils.DateUtils
import kotlinx.coroutines.launch

class StatsViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _shopStats = MutableLiveData<List<ShopStat>>()
    val shopStats: LiveData<List<ShopStat>> = _shopStats

    init {
        loadShopStats()
    }

    private fun loadShopStats() {
        viewModelScope.launch {
            val stats = repository.getShopStats(DateUtils.getStartOfMonth())
            _shopStats.postValue(stats)
        }
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

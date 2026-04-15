package com.example.budgettracker.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.model.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.utils.DateUtils
import kotlinx.coroutines.launch

class MainViewModel(private val repository: BudgetRepository) : ViewModel() {

    val transactions = repository.getAllTransactions().asLiveData()

    private val _totalExpense = MutableLiveData<Double>()
    val totalExpense: LiveData<Double> = _totalExpense

    init {
        loadTotalExpense()
    }

    private fun loadTotalExpense() {
        viewModelScope.launch {
            val expense = repository.getTotalExpenseSince(DateUtils.getStartOfMonth())
            _totalExpense.postValue(expense)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        repository.deleteTransaction(transaction)
        loadTotalExpense() // обновим после удаления
    }

    class MainViewModelFactory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

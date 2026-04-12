package com.example.budgettracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.model.Transaction
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class AddViewModel(private val repository: BudgetRepository) : ViewModel() {

    fun addTransaction(amount: Double, shop: String, category: String, date: Long, note: String?) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                shop = shop,
                category = category,
                date = date,
                note = note
            )
            repository.insertTransaction(transaction)
        }
    }

    class AddViewModelFactory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AddViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

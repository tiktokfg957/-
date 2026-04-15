package com.example.budgettracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettracker.databinding.ActivityMainBinding
import com.example.budgettracker.ui.add.AddActivity
import com.example.budgettracker.ui.stats.StatsActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((application as com.example.budgettracker.BudgetApplication).repository)
    }
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        setupRecyclerView()
        observeData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            lifecycleScope.launch {
                viewModel.deleteTransaction(transaction)
                Toast.makeText(this@MainActivity, "Удалено", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun observeData() {
        viewModel.transactions.observe(this) { transactions ->
            adapter.submitList(transactions)
        }
        viewModel.totalExpense.observe(this) { expense ->
            binding.tvTotalExpense.text = String.format("%.2f ₽", expense)
            val budget = 30000.0
            val balance = budget - expense
            binding.tvBalance.text = String.format("%.2f ₽", balance)
            if (balance < 0) {
                binding.tvAdvice.text = "Вы превысили бюджет! Пора экономить."
            } else if (balance < 5000) {
                binding.tvAdvice.text = "Осталось меньше 5000 ₽. Будьте внимательны."
            } else {
                binding.tvAdvice.text = "Хорошо! Вы укладываетесь в бюджет."
            }
        }
    }

    private fun setupListeners() {
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }
        binding.btnStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }
}

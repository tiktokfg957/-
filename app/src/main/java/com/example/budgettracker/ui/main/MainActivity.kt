package com.example.budgettracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.R
import com.example.budgettracker.ui.add.AddActivity
import com.example.budgettracker.ui.stats.StatsActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvBalance: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnStats: MaterialButton

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((application as com.example.budgettracker.BudgetApplication).repository)
    }
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.app_name)

        rvTransactions = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAdvice = findViewById(R.id.tvAdvice)
        btnAdd = findViewById(R.id.btnAdd)
        btnStats = findViewById(R.id.btnStats)

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
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter
    }

    private fun observeData() {
        viewModel.transactions.observe(this) { transactions ->
            adapter.submitList(transactions)
        }
        viewModel.totalExpense.observe(this) { expense ->
            tvTotalExpense.text = String.format("%.2f ₽", expense)
            val budget = 30000.0
            val balance = budget - expense
            tvBalance.text = String.format("%.2f ₽", balance)
            tvAdvice.text = when {
                balance < 0 -> "Вы превысили бюджет! Пора экономить."
                balance < 5000 -> "Осталось меньше 5000 ₽. Будьте внимательны."
                else -> "Хорошо! Вы укладываетесь в бюджет."
            }
        }
    }

    private fun setupListeners() {
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }
}

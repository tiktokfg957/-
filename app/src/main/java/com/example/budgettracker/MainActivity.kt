package com.example.budgettracker

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var btnFilter: Button

    private val transactions = mutableListOf<Transaction>()
    private var totalIncome = 0.0
    private var totalExpense = 0.0
    private var budget = 30000.0
    private var currentFilter = "all" // all, week, month, year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("budget_data", Context.MODE_PRIVATE)
        budget = prefs.getFloat("budget", 30000f).toDouble()

        recyclerView = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAdvice = findViewById(R.id.tvAdvice)
        btnFilter = findViewById(R.id.btnFilter)
        val btnStats = findViewById<Button>(R.id.btnStats)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        adapter = TransactionAdapter(transactions) { position ->
            removeTransaction(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadData()
        updateUI()
        applyFilter()

        btnAdd.setOnClickListener { showAddDialog() }
        btnStats.setOnClickListener { showStatsDialog() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        btnFilter.setOnClickListener { showFilterDialog() }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        AlertDialog.Builder(this)
            .setTitle("Новая операция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val date = etDate.text.toString()
                val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"
                if (amount != null && amount > 0 && shop.isNotEmpty() && category.isNotEmpty()) {
                    addTransaction(amount, shop, category, date, type)
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addTransaction(amount: Double, shop: String, category: String, date: String, type: String) {
        val transaction = Transaction(amount, shop, category, date, type)
        transactions.add(0, transaction)
        if (type == "income") totalIncome += amount else totalExpense += amount
        saveData()
        applyFilter()
        updateUI()
    }

    private fun removeTransaction(position: Int) {
        val transaction = transactions[position]
        if (transaction.type == "income") totalIncome -= transaction.amount else totalExpense -= transaction.amount
        transactions.removeAt(position)
        saveData()
        applyFilter()
        updateUI()
    }

    private fun updateUI() {
        val balance = totalIncome - totalExpense
        tvBalance.text = String.format("%.2f ₽", balance)
        tvTotalIncome.text = String.format("Доходы: %.2f ₽", totalIncome)
        tvTotalExpense.text = String.format("Расходы: %.2f ₽", totalExpense)

        val balanceRelative = totalIncome - totalExpense
        val advice = when {
            balanceRelative < 0 -> "⚠️ Вы превысили бюджет на ${String.format("%.2f", -balanceRelative)} ₽"
            (budget - totalExpense) < 5000 -> "❗ Осталось меньше 5000 ₽ бюджета. Будьте внимательны."
            else -> "✅ Вы укладываетесь в бюджет. Отлично!"
        }
        tvAdvice.text = advice
    }

    private fun applyFilter() {
        val now = Calendar.getInstance()
        val filtered = when (currentFilter) {
            "week" -> {
                val weekAgo = now.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                transactions.filter { parseDateToTimestamp(it.date) >= weekAgo }
            }
            "month" -> {
                val monthAgo = now.apply { add(Calendar.MONTH, -1) }.timeInMillis
                transactions.filter { parseDateToTimestamp(it.date) >= monthAgo }
            }
            "year" -> {
                val yearAgo = now.apply { add(Calendar.YEAR, -1) }.timeInMillis
                transactions.filter { parseDateToTimestamp(it.date) >= yearAgo }
            }
            else -> transactions
        }
        adapter.submitList(filtered)
    }

    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: 0
        } catch (e: Exception) { 0 }
    }

    private fun showFilterDialog() {
        val options = arrayOf("Все", "Неделя", "Месяц", "Год")
        val currentIndex = when (currentFilter) {
            "week" -> 1
            "month" -> 2
            "year" -> 3
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle("Показать операции")
            .setSingleChoiceItems(options, currentIndex) { _, which ->
                currentFilter = when (which) {
                    1 -> "week"
                    2 -> "month"
                    3 -> "year"
                    else -> "all"
                }
                applyFilter()
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showStatsDialog() {
        val expenses = transactions.filter { it.type == "expense" }
        if (expenses.isEmpty()) {
            Toast.makeText(this, "Нет расходов для статистики", Toast.LENGTH_SHORT).show()
            return
        }
        val categoryMap = expenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val entries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Расходы по категориям")
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        val pieData = PieData(dataSet)

        val dialogView = layoutInflater.inflate(R.layout.dialog_stats, null)
        val pieChart = dialogView.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.pieChart)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = "Расходы"
        pieChart.setCenterTextSize(16f)
        pieChart.invalidate()

        AlertDialog.Builder(this)
            .setTitle("Статистика")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        etBudget.setText(budget.toInt().toString())

        AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newBudget = etBudget.text.toString().toDoubleOrNull()
                if (newBudget != null && newBudget > 0) {
                    budget = newBudget
                    prefs.edit().putFloat("budget", budget.toFloat()).apply()
                    updateUI()
                    Toast.makeText(this, "Бюджет сохранён", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Некорректная сумма", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveData() {
        val jsonArray = JSONArray()
        transactions.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("shop", it.shop)
            obj.put("category", it.category)
            obj.put("date", it.date)
            obj.put("type", it.type)
            jsonArray.put(obj)
        }
        prefs.edit().putString("transactions", jsonArray.toString()).apply()
        prefs.edit().putFloat("totalIncome", totalIncome.toFloat()).apply()
        prefs.edit().putFloat("totalExpense", totalExpense.toFloat()).apply()
    }

    private fun loadData() {
        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        transactions.clear()
        totalIncome = 0.0
        totalExpense = 0.0
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val transaction = Transaction(
                obj.getDouble("amount"),
                obj.getString("shop"),
                obj.getString("category"),
                obj.getString("date"),
                obj.getString("type")
            )
            transactions.add(transaction)
            if (transaction.type == "income") totalIncome += transaction.amount else totalExpense += transaction.amount
        }
        // fallback для старых данных без типа
        if (jsonArray.length() > 0 && !jsonArray.getJSONObject(0).has("type")) {
            // миграция: все старые транзакции считаем расходами
            totalIncome = 0.0
            totalExpense = transactions.sumOf { it.amount }
        }
    }

    data class Transaction(val amount: Double, val shop: String, val category: String, val date: String, val type: String)

    inner class TransactionAdapter(
        private var items: List<Transaction>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        fun submitList(list: List<Transaction>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvShop.text = item.shop
            holder.tvCategory.text = item.category
            val prefix = if (item.type == "income") "+" else "-"
            holder.tvAmount.text = String.format("%s%.2f ₽", prefix, item.amount)
            holder.tvAmount.setTextColor(if (item.type == "income") android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336"))
            holder.tvDate.text = item.date
            holder.btnDelete.setOnClickListener { onDelete(position) }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvShop: TextView = itemView.findViewById(R.id.tvShop)
            val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
            val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        }
    }
}

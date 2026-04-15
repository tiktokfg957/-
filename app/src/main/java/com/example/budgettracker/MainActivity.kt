package com.example.budgettracker

import android.content.ContentValues
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.opencsv.CSVWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var etSearch: EditText
    private lateinit var spinnerSort: Spinner

    private val allTransactions = mutableListOf<Transaction>()
    private var transactions = mutableListOf<Transaction>()
    private var totalExpense = 0.0
    private var budget = 30000.0
    private var currentSort = "date_desc"

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        recyclerView = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAdvice = findViewById(R.id.tvAdvice)
        etSearch = findViewById(R.id.etSearch)
        spinnerSort = findViewById(R.id.spinnerSort)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        setupSortSpinner()
        setupSearch()

        adapter = TransactionAdapter(transactions) { position ->
            removeTransaction(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadData()
        applyFiltersAndSort()
        updateUI()

        btnAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Сначала новые", "Сначала старые", "По сумме (возр.)", "По сумме (убыв.)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSort = when (position) {
                    0 -> "date_desc"
                    1 -> "date_asc"
                    2 -> "amount_asc"
                    3 -> "amount_desc"
                    else -> "date_desc"
                }
                applyFiltersAndSort()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFiltersAndSort()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun applyFiltersAndSort() {
        val query = etSearch.text.toString().trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allTransactions.toList()
        } else {
            allTransactions.filter {
                it.shop.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.date.contains(query)
            }
        }
        transactions.clear()
        transactions.addAll(sortTransactions(filtered))
        adapter.notifyDataSetChanged()
        updateUI()
    }

    private fun sortTransactions(list: List<Transaction>): List<Transaction> {
        return when (currentSort) {
            "date_asc" -> list.sortedBy { parseDate(it.date) }
            "amount_asc" -> list.sortedBy { it.amount }
            "amount_desc" -> list.sortedByDescending { it.amount }
            else -> list.sortedByDescending { parseDate(it.date) }
        }
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: 0
        } catch (e: Exception) { 0 }
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
        allTransactions.add(0, transaction)
        if (type == "expense") totalExpense += amount
        saveData()
        applyFiltersAndSort()
        updateUI()
    }

    private fun removeTransaction(position: Int) {
        val transaction = transactions[position]
        if (transaction.type == "expense") totalExpense -= transaction.amount
        allTransactions.remove(transaction)
        saveData()
        applyFiltersAndSort()
        updateUI()
    }

    private fun updateUI() {
        val balance = budget - totalExpense
        tvBalance.text = String.format("%.2f ₽", balance)
        tvTotalExpense.text = String.format("Расходы: %.2f ₽", totalExpense)

        val advice = when {
            balance < 0 -> "⚠️ Вы превысили бюджет на ${String.format("%.2f", -balance)} ₽"
            balance < 5000 -> "❗ Осталось меньше 5000 ₽. Будьте внимательны."
            else -> "✅ Вы укладываетесь в бюджет. Отлично!"
        }
        tvAdvice.text = advice
    }

    private fun exportToCSV() {
        try {
            val file = File(getExternalFilesDir(null), "transactions_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                CSVWriter(writer).use { csvWriter ->
                    csvWriter.writeNext(arrayOf("Тип", "Сумма", "Магазин", "Категория", "Дата"))
                    allTransactions.forEach {
                        csvWriter.writeNext(arrayOf(
                            if (it.type == "income") "Доход" else "Расход",
                            it.amount.toString(),
                            it.shop,
                            it.category,
                            it.date
                        ))
                    }
                }
            }
            Toast.makeText(this, "CSV сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_budget -> {
                showBudgetDialog()
                true
            }
            R.id.action_export -> {
                exportToCSV()
                true
            }
            R.id.action_theme -> {
                toggleTheme()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        etBudget.setText(budget.toInt().toString())
        AlertDialog.Builder(this)
            .setTitle("Настройка бюджета")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newBudget = etBudget.text.toString().toDoubleOrNull()
                if (newBudget != null && newBudget > 0) {
                    budget = newBudget
                    prefs.edit().putFloat("budget", budget.toFloat()).apply()
                    updateUI()
                } else {
                    Toast.makeText(this, "Некорректная сумма", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toggleTheme() {
        val isDark = prefs.getBoolean("dark_theme", false)
        prefs.edit().putBoolean("dark_theme", !isDark).apply()
        applyTheme()
        recreate()
    }

    private fun applyTheme() {
        val isDark = prefs.getBoolean("dark_theme", false)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun saveData() {
        val jsonArray = JSONArray()
        allTransactions.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("shop", it.shop)
            obj.put("category", it.category)
            obj.put("date", it.date)
            obj.put("type", it.type)
            jsonArray.put(obj)
        }
        prefs.edit().putString("transactions", jsonArray.toString()).apply()
        prefs.edit().putFloat("totalExpense", totalExpense.toFloat()).apply()
        prefs.edit().putFloat("budget", budget.toFloat()).apply()
    }

    private fun loadData() {
        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        allTransactions.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val transaction = Transaction(
                obj.getDouble("amount"),
                obj.getString("shop"),
                obj.getString("category"),
                obj.getString("date"),
                obj.getString("type")
            )
            allTransactions.add(transaction)
        }
        totalExpense = prefs.getFloat("totalExpense", 0f).toDouble()
        budget = prefs.getFloat("budget", 30000f).toDouble()
    }

    data class Transaction(val amount: Double, val shop: String, val category: String, val date: String, val type: String)

    inner class TransactionAdapter(
        private val items: List<Transaction>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

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
            holder.tvAmount.setTextColor(if (item.type == "income") Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
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

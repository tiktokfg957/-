package com.example.budgettracker

import android.app.DatePickerDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var btnFilter: Button

    private val transactions = mutableListOf<Transaction>()
    private var budget = 30000.0
    private var currentFilter = "all" // all, week, month, year
    private var categoryLimits = mutableMapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        recyclerView = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAdvice = findViewById(R.id.tvAdvice)
        btnFilter = findViewById(R.id.btnFilter)
        val btnStats = findViewById<Button>(R.id.btnStats)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnBackup = findViewById<Button>(R.id.btnBackup)
        val btnRestore = findViewById<Button>(R.id.btnRestore)
        val fabAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        loadBudget()
        loadCategoryLimits()
        loadData()
        updateFilteredList()

        adapter = TransactionAdapter(transactions,
            onLongClick = { position -> showEditDialog(position) },
            onDelete = { position -> deleteTransaction(position) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        updateUI()

        btnFilter.setOnClickListener {
            showFilterDialog()
        }
        btnStats.setOnClickListener {
            startActivity(android.content.Intent(this, StatsActivity::class.java))
        }
        btnSettings.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
        btnBackup.setOnClickListener {
            exportData()
        }
        btnRestore.setOnClickListener {
            importData()
        }
        fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf("Все", "Неделя", "Месяц", "Год")
        AlertDialog.Builder(this)
            .setTitle("Фильтр по дате")
            .setItems(options) { _, which ->
                currentFilter = when (which) {
                    0 -> "all"
                    1 -> "week"
                    2 -> "month"
                    3 -> "year"
                    else -> "all"
                }
                updateFilteredList()
                adapter.submitList(transactions)
                updateUI()
            }
            .show()
    }

    private fun updateFilteredList() {
        val now = Calendar.getInstance()
        val filtered = when (currentFilter) {
            "week" -> {
                val weekAgo = now.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                transactions.filter { it.dateTimestamp >= weekAgo }
            }
            "month" -> {
                val monthAgo = now.apply { add(Calendar.MONTH, -1) }.timeInMillis
                transactions.filter { it.dateTimestamp >= monthAgo }
            }
            "year" -> {
                val yearAgo = now.apply { add(Calendar.YEAR, -1) }.timeInMillis
                transactions.filter { it.dateTimestamp >= yearAgo }
            }
            else -> transactions
        }
        adapter.submitList(filtered)
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = dialogView.findViewById<RadioButton>(R.id.rbIncome)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))
        etDate.setOnClickListener { showDatePicker(etDate) }

        AlertDialog.Builder(this)
            .setTitle("Новая операция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val dateStr = etDate.text.toString()
                val type = if (rbIncome.isChecked) "income" else "expense"
                if (amount != null && amount > 0 && shop.isNotEmpty() && category.isNotEmpty()) {
                    val dateTimestamp = parseDate(dateStr)
                    addTransaction(amount, shop, category, dateTimestamp, type)
                    // Проверка лимита категории
                    if (type == "expense") {
                        checkCategoryLimit(category)
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(position: Int) {
        val transaction = transactions[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = dialogView.findViewById<RadioButton>(R.id.rbIncome)

        etAmount.setText(transaction.amount.toString())
        etShop.setText(transaction.shop)
        etCategory.setText(transaction.category)
        etDate.setText(transaction.date)
        if (transaction.type == "income") rbIncome.isChecked = true else rbExpense.isChecked = true
        etDate.setOnClickListener { showDatePicker(etDate) }

        AlertDialog.Builder(this)
            .setTitle("Редактировать")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val dateStr = etDate.text.toString()
                val type = if (rbIncome.isChecked) "income" else "expense"
                if (amount != null && amount > 0 && shop.isNotEmpty() && category.isNotEmpty()) {
                    val dateTimestamp = parseDate(dateStr)
                    transaction.amount = amount
                    transaction.shop = shop
                    transaction.category = category
                    transaction.dateTimestamp = dateTimestamp
                    transaction.date = dateStr
                    transaction.type = type
                    saveData()
                    updateFilteredList()
                    adapter.notifyDataSetChanged()
                    updateUI()
                    if (type == "expense") checkCategoryLimit(category)
                    Toast.makeText(this, "Обновлено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteTransaction(position: Int) {
        val transaction = transactions[position]
        transactions.removeAt(position)
        saveData()
        updateFilteredList()
        adapter.notifyDataSetChanged()
        updateUI()
    }

    private fun addTransaction(amount: Double, shop: String, category: String, dateTimestamp: Long, type: String) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(dateTimestamp))
        val transaction = Transaction(amount, shop, category, dateStr, dateTimestamp, type)
        transactions.add(0, transaction)
        saveData()
        updateFilteredList()
        adapter.notifyItemInserted(0)
        updateUI()
    }

    private fun checkCategoryLimit(category: String) {
        val limit = categoryLimits[category] ?: return
        val totalExpenseForCategory = transactions.filter { it.category == category && it.type == "expense" }.sumOf { it.amount }
        if (totalExpenseForCategory > limit) {
            Toast.makeText(this, "Превышен лимит на категорию $category! (${totalExpenseForCategory} / $limit)", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUI() {
        val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        tvBalance.text = String.format("%.2f ₽", balance)
        tvTotalExpense.text = String.format("Расходы: %.2f ₽", totalExpense)

        val advice = when {
            balance < 0 -> "⚠️ Вы превысили бюджет на ${String.format("%.2f", -balance)} ₽"
            balance < 5000 -> "❗ Осталось меньше 5000 ₽. Будьте внимательны."
            else -> "✅ Вы укладываетесь в бюджет. Отлично!"
        }
        tvAdvice.text = advice
    }

    private fun loadBudget() {
        budget = prefs.getFloat("budget", 30000f).toDouble()
    }

    private fun loadCategoryLimits() {
        val json = prefs.getString("category_limits", "{}")
        val obj = JSONObject(json)
        categoryLimits.clear()
        obj.keys().forEach { key ->
            categoryLimits[key] = obj.getDouble(key)
        }
    }

    private fun saveCategoryLimits() {
        val obj = JSONObject()
        categoryLimits.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("category_limits", obj.toString()).apply()
    }

    private fun saveData() {
        val jsonArray = JSONArray()
        transactions.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("shop", it.shop)
            obj.put("category", it.category)
            obj.put("date", it.date)
            obj.put("timestamp", it.dateTimestamp)
            obj.put("type", it.type)
            jsonArray.put(obj)
        }
        prefs.edit().putString("transactions", jsonArray.toString()).apply()
    }

    private fun loadData() {
        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        transactions.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val transaction = Transaction(
                obj.getDouble("amount"),
                obj.getString("shop"),
                obj.getString("category"),
                obj.getString("date"),
                obj.getLong("timestamp"),
                obj.getString("type")
            )
            transactions.add(transaction)
        }
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            editText.setText(String.format("%02d.%02d.%04d", day, month + 1, year))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun exportData() {
        val json = prefs.getString("transactions", "[]")
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_TEXT, json)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "budget_backup.json")
        }
        startActivity(android.content.Intent.createChooser(intent, "Экспорт данных"))
    }

    private fun importData() {
        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data
            uri?.let {
                contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { it.readText() }
                    prefs.edit().putString("transactions", json).apply()
                    loadData()
                    updateFilteredList()
                    adapter.notifyDataSetChanged()
                    updateUI()
                    Toast.makeText(this, "Данные восстановлены", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    data class Transaction(
        var amount: Double,
        var shop: String,
        var category: String,
        var date: String,
        var dateTimestamp: Long,
        var type: String // "income" or "expense"
    )

    inner class TransactionAdapter(
        private val items: List<Transaction>,
        private val onLongClick: (Int) -> Unit,
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
            val sign = if (item.type == "income") "+" else "-"
            holder.tvAmount.text = String.format("%s%.2f ₽", sign, item.amount)
            holder.tvAmount.setTextColor(if (item.type == "income") android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336"))
            holder.tvDate.text = item.date
            holder.btnDelete.setOnClickListener { onDelete(position) }
            holder.itemView.setOnLongClickListener {
                onLongClick(position)
                true
            }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_stats -> {
                startActivity(android.content.Intent(this, StatsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

package com.example.budgettracker

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvAdvice: TextView

    private val transactions = mutableListOf<Transaction>()
    private var totalExpense = 0.0
    private var totalIncome = 0.0
    private var budget = 30000.0
    private var currentFilter = "all" // all, week, month, year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)
        budget = prefs.getFloat("budget", 30000f).toDouble()

        recyclerView = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvAdvice = findViewById(R.id.tvAdvice)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        adapter = TransactionAdapter(transactions) { position ->
            removeTransaction(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadData()
        applyFilter()
        updateUI()

        btnAdd.setOnClickListener {
            showAddDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showBudgetDialog()
                true
            }
            R.id.action_stats -> {
                startActivity(android.content.Intent(this, StatsActivity::class.java))
                true
            }
            R.id.action_filter_week -> {
                currentFilter = "week"
                applyFilter()
                true
            }
            R.id.action_filter_month -> {
                currentFilter = "month"
                applyFilter()
                true
            }
            R.id.action_filter_year -> {
                currentFilter = "year"
                applyFilter()
                true
            }
            R.id.action_filter_all -> {
                currentFilter = "all"
                applyFilter()
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
            .setTitle("Установить бюджет")
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

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbExpense)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        AlertDialog.Builder(this)
            .setTitle("Новая операция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val date = etDate.text.toString()
                val type = if (rgType.checkedRadioButtonId == R.id.rbExpense) "expense" else "income"
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
        if (type == "expense") totalExpense += amount else totalIncome += amount
        saveData()
        applyFilter() // перерисовка с текущим фильтром
        updateUI()
    }

    private fun removeTransaction(position: Int) {
        val transaction = transactions[position]
        if (transaction.type == "expense") totalExpense -= transaction.amount
        else totalIncome -= transaction.amount
        transactions.removeAt(position)
        saveData()
        applyFilter()
        updateUI()
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
        adapter.updateList(filtered)
    }

    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: 0
        } catch (e: Exception) { 0 }
    }

    private fun updateUI() {
        val balance = totalIncome - totalExpense
        tvBalance.text = String.format("%.2f ₽", balance)
        tvTotalExpense.text = String.format("Расходы: %.2f ₽", totalExpense)
        tvTotalIncome.text = String.format("Доходы: %.2f ₽", totalIncome)

        val remainingBudget = budget - totalExpense
        val advice = when {
            remainingBudget < 0 -> "⚠️ Вы превысили бюджет на ${String.format("%.2f", -remainingBudget)} ₽"
            remainingBudget < 5000 -> "❗ Осталось меньше 5000 ₽. Будьте внимательны."
            else -> "✅ Вы укладываетесь в бюджет. Отлично!"
        }
        tvAdvice.text = advice
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
        prefs.edit().putFloat("totalExpense", totalExpense.toFloat()).apply()
        prefs.edit().putFloat("totalIncome", totalIncome.toFloat()).apply()
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
                obj.optString("type", "expense")
            )
            transactions.add(transaction)
        }
        totalExpense = prefs.getFloat("totalExpense", 0f).toDouble()
        totalIncome = prefs.getFloat("totalIncome", 0f).toDouble()
    }

    data class Transaction(
        val amount: Double,
        val shop: String,
        val category: String,
        val date: String,
        val type: String = "expense"
    )

    inner class TransactionAdapter(
        private var items: List<Transaction>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        fun updateList(newItems: List<Transaction>) {
            items = newItems
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
            val prefix = if (item.type == "expense") "-" else "+"
            holder.tvAmount.text = "$prefix${String.format("%.2f ₽", item.amount)}"
            holder.tvAmount.setTextColor(if (item.type == "expense") 0xFFF44336.toInt() else 0xFF4CAF50.toInt())
            holder.tvDate.text = item.date
            holder.btnDelete.setOnClickListener { onDelete(adapterPosition) }
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

package com.example.budgettracker

import android.content.SharedPreferences
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

    private val transactions = mutableListOf<Transaction>()
    private var totalExpense = 0.0   // сумма расходов
    private var totalIncome = 0.0    // сумма доходов
    private var budget = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        recyclerView = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAdvice = findViewById(R.id.tvAdvice)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        adapter = TransactionAdapter(transactions) { position ->
            removeTransaction(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadData()

        // Проверяем, установлен ли бюджет
        val budgetSet = prefs.getBoolean("budget_set", false)
        if (!budgetSet) {
            showBudgetDialog()
        } else {
            budget = prefs.getFloat("budget", 30000f).toDouble()
            updateUI()
        }

        btnAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveBudget)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Введите ваш бюджет на месяц")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnSave.setOnClickListener {
            val budgetStr = etBudget.text.toString().trim()
            if (budgetStr.isNotEmpty()) {
                val newBudget = budgetStr.toDoubleOrNull()
                if (newBudget != null && newBudget > 0) {
                    budget = newBudget
                    prefs.edit().putFloat("budget", budget.toFloat()).apply()
                    prefs.edit().putBoolean("budget_set", true).apply()
                    updateUI()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Введите корректную сумму", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Поле не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = dialogView.findViewById<RadioButton>(R.id.rbIncome)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        AlertDialog.Builder(this)
            .setTitle("Новая операция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val date = etDate.text.toString()
                val type = if (rbIncome.isChecked) "income" else "expense"

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
        if (type == "expense") {
            totalExpense += amount
        } else {
            totalIncome += amount
        }
        saveData()
        adapter.notifyItemInserted(0)
        updateUI()
    }

    private fun removeTransaction(position: Int) {
        val transaction = transactions[position]
        if (transaction.type == "expense") {
            totalExpense -= transaction.amount
        } else {
            totalIncome -= transaction.amount
        }
        transactions.removeAt(position)
        saveData()
        adapter.notifyItemRemoved(position)
        updateUI()
    }

    private fun updateUI() {
        val balance = budget - totalExpense + totalIncome
        tvBalance.text = String.format("%.2f ₽", balance)
        tvTotalExpense.text = String.format("Расходы: %.2f ₽ | Доходы: %.2f ₽", totalExpense, totalIncome)

        val advice = when {
            balance < 0 -> "⚠️ Вы превысили бюджет на ${String.format("%.2f", -balance)} ₽"
            balance < 5000 -> "❗ Осталось меньше 5000 ₽. Будьте внимательны."
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
                obj.getString("type")
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
        val type: String  // "income" или "expense"
    )

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
            holder.tvAmount.text = String.format("%.2f ₽", item.amount)
            holder.tvDate.text = item.date
            holder.btnDelete.setOnClickListener { onDelete(position) }

            // Цвет в зависимости от типа
            if (item.type == "income") {
                holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#F44336"))
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
}

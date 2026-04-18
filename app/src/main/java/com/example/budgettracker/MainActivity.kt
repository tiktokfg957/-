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
import com.yandex.mobile.ads.banner.*
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
    private lateinit var adContainer: FrameLayout
    private var bannerAdView: BannerAdView? = null

    private val transactions = mutableListOf<Transaction>()
    private var totalExpense = 0.0
    private val budget = 30000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        recyclerView = findViewById(R.id.rvTransactions)
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAdvice = findViewById(R.id.tvAdvice)
        adContainer = findViewById(R.id.adContainer)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        adapter = TransactionAdapter(transactions) { position ->
            removeTransaction(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadData()
        updateUI()

        btnAdd.setOnClickListener {
            showAddDialog()
        }

        loadBannerAd()
    }

    private fun loadBannerAd() {
        val adUnitId = "R-M-19097965-1"  // твой ID
        bannerAdView = BannerAdView(this)
        bannerAdView?.setAdUnitId(adUnitId)

        val adSize = BannerAdSize.stickySize(this, adContainer.width)
        bannerAdView?.setAdSize(adSize)

        bannerAdView?.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {
                adContainer.removeAllViews()
                adContainer.addView(bannerAdView)
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                adContainer.visibility = View.GONE
            }

            override fun onAdClicked() {}
            override fun onLeftApplication() {}
            override fun onReturnedToApplication() {}
            override fun onImpression(impressionData: ImpressionData?) {}
        })

        bannerAdView?.loadAd(AdRequest.Builder().build())
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAdView?.destroy()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        AlertDialog.Builder(this)
            .setTitle("Новая трата")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val date = etDate.text.toString()
                if (amount != null && amount > 0 && shop.isNotEmpty() && category.isNotEmpty()) {
                    addTransaction(amount, shop, category, date)
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addTransaction(amount: Double, shop: String, category: String, date: String) {
        val transaction = Transaction(amount, shop, category, date)
        transactions.add(0, transaction)
        totalExpense += amount
        saveData()
        adapter.notifyItemInserted(0)
        updateUI()
    }

    private fun removeTransaction(position: Int) {
        val transaction = transactions[position]
        totalExpense -= transaction.amount
        transactions.removeAt(position)
        saveData()
        adapter.notifyItemRemoved(position)
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

    private fun saveData() {
        val jsonArray = JSONArray()
        transactions.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("shop", it.shop)
            obj.put("category", it.category)
            obj.put("date", it.date)
            jsonArray.put(obj)
        }
        prefs.edit().putString("transactions", jsonArray.toString()).apply()
        prefs.edit().putFloat("totalExpense", totalExpense.toFloat()).apply()
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
                obj.getString("date")
            )
            transactions.add(transaction)
        }
        totalExpense = prefs.getFloat("totalExpense", 0f).toDouble()
    }

    data class Transaction(val amount: Double, val shop: String, val category: String, val date: String)

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

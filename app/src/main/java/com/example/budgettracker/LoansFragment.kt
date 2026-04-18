package com.example.budgettracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LoansFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LoanAdapter
    private val loans = mutableListOf<Loan>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_loans, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireActivity().getSharedPreferences("budget_data", Context.MODE_PRIVATE)

        recyclerView = view.findViewById(R.id.rvLoans)
        val btnAdd = view.findViewById<Button>(R.id.btnAddLoan)

        adapter = LoanAdapter(loans) { loan, action ->
            when (action) {
                "pay" -> showPayDialog(loan)
                "delete" -> deleteLoan(loan)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadLoans()

        btnAdd.setOnClickListener {
            showAddLoanDialog()
        }
    }

    private fun showAddLoanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loan, null)
        val etTotal = dialogView.findViewById<EditText>(R.id.etLoanTotal)
        val etMonthly = dialogView.findViewById<EditText>(R.id.etMonthlyPayment)
        val etName = dialogView.findViewById<EditText>(R.id.etLoanName)

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить кредит")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val totalStr = etTotal.text.toString()
                val monthlyStr = etMonthly.text.toString()
                val name = etName.text.toString().trim()
                if (totalStr.isNotEmpty() && monthlyStr.isNotEmpty() && name.isNotEmpty()) {
                    val total = totalStr.toDoubleOrNull()
                    val monthly = monthlyStr.toDoubleOrNull()
                    if (total != null && monthly != null && total > 0 && monthly > 0) {
                        addLoan(name, total, monthly, total)
                    } else {
                        Toast.makeText(requireContext(), "Некорректные суммы", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addLoan(name: String, total: Double, monthly: Double, remaining: Double) {
        val loan = Loan(name, total, monthly, remaining)
        loans.add(loan)
        saveLoans()
        adapter.notifyItemInserted(loans.size - 1)
    }

    private fun showPayDialog(loan: Loan) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pay_loan, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etPaymentAmount)

        AlertDialog.Builder(requireContext())
            .setTitle("Платеж по кредиту: ${loan.name}")
            .setView(dialogView)
            .setPositiveButton("Оплатить") { _, _ ->
                val amountStr = etAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0 && amount <= loan.remaining) {
                        // Списываем деньги из бюджета (обновляем SharedPreferences)
                        val budgetPrefs = requireActivity().getSharedPreferences("budget_data", Context.MODE_PRIVATE)
                        var budget = budgetPrefs.getFloat("budget", 30000f).toDouble()
                        val totalExpense = budgetPrefs.getFloat("totalExpense", 0f).toDouble()
                        val totalIncome = budgetPrefs.getFloat("totalIncome", 0f).toDouble()

                        // Добавляем расход в транзакции (как трату)
                        val transactionsPrefs = budgetPrefs.getString("transactions", "[]") ?: "[]"
                        val jsonArray = JSONArray(transactionsPrefs)
                        val newTransaction = JSONObject().apply {
                            put("amount", amount)
                            put("shop", "Платеж по кредиту: ${loan.name}")
                            put("category", "Кредит")
                            put("date", SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))
                            put("type", "expense")
                        }
                        jsonArray.put(newTransaction)
                        budgetPrefs.edit().putString("transactions", jsonArray.toString()).apply()
                        budgetPrefs.edit().putFloat("totalExpense", (totalExpense + amount).toFloat()).apply()

                        // Уменьшаем остаток кредита
                        loan.remaining -= amount
                        saveLoans()
                        adapter.notifyDataSetChanged()

                        // Обновляем бюджет (перезагрузка фрагмента транзакций)
                        Toast.makeText(requireContext(), "Платеж зачислен. Остаток: ${loan.remaining}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Введите сумму", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteLoan(loan: Loan) {
        loans.remove(loan)
        saveLoans()
        adapter.notifyDataSetChanged()
    }

    private fun saveLoans() {
        val jsonArray = JSONArray()
        loans.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("total", it.total)
            obj.put("monthly", it.monthly)
            obj.put("remaining", it.remaining)
            jsonArray.put(obj)
        }
        prefs.edit().putString("loans", jsonArray.toString()).apply()
    }

    private fun loadLoans() {
        val jsonStr = prefs.getString("loans", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        loans.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val loan = Loan(
                obj.getString("name"),
                obj.getDouble("total"),
                obj.getDouble("monthly"),
                obj.getDouble("remaining")
            )
            loans.add(loan)
        }
        adapter.notifyDataSetChanged()
    }

    data class Loan(val name: String, val total: Double, val monthly: Double, var remaining: Double)

    inner class LoanAdapter(
        private val items: List<Loan>,
        private val onAction: (Loan, String) -> Unit
    ) : RecyclerView.Adapter<LoanAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loan, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val loan = items[position]
            holder.tvName.text = loan.name
            holder.tvTotal.text = "Общая сумма: ${String.format("%.2f", loan.total)} ₽"
            holder.tvMonthly.text = "Ежемесячный: ${String.format("%.2f", loan.monthly)} ₽"
            holder.tvRemaining.text = "Остаток: ${String.format("%.2f", loan.remaining)} ₽"
            holder.btnPay.setOnClickListener { onAction(loan, "pay") }
            holder.btnDelete.setOnClickListener { onAction(loan, "delete") }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvLoanName)
            val tvTotal: TextView = itemView.findViewById(R.id.tvLoanTotal)
            val tvMonthly: TextView = itemView.findViewById(R.id.tvLoanMonthly)
            val tvRemaining: TextView = itemView.findViewById(R.id.tvLoanRemaining)
            val btnPay: Button = itemView.findViewById(R.id.btnPayLoan)
            val btnDelete: Button = itemView.findViewById(R.id.btnDeleteLoan)
        }
    }
}

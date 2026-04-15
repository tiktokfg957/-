package com.example.budgettracker.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.R
import com.example.budgettracker.data.model.Transaction
import com.example.budgettracker.utils.DateUtils

class TransactionAdapter(
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var transactions = listOf<Transaction>()

    fun submitList(list: List<Transaction>) {
        transactions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transactions[position])
        holder.itemView.setOnLongClickListener {
            onLongClick(transactions[position])
            true
        }
    }

    override fun getItemCount() = transactions.size

    inner class ViewHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView) {
        private val tvShop: TextView = itemView.findViewById(R.id.tvShop)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(transaction: Transaction) {
            tvShop.text = transaction.shop
            tvCategory.text = transaction.category
            tvAmount.text = String.format("%.2f ₽", transaction.amount)
            tvDate.text = DateUtils.timestampToString(transaction.date)
            if (transaction.amount > 1000) {
                tvAmount.setTextColor(android.graphics.Color.parseColor("#F44336"))
            } else {
                tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
        }
    }
}

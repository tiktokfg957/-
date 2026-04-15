package com.example.budgettracker.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.databinding.ItemTransactionBinding
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
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transactions[position])
        holder.itemView.setOnLongClickListener {
            onLongClick(transactions[position])
            true
        }
    }

    override fun getItemCount() = transactions.size

    inner class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.tvShop.text = transaction.shop
            binding.tvCategory.text = transaction.category
            binding.tvAmount.text = String.format("%.2f ₽", transaction.amount)
            binding.tvDate.text = DateUtils.timestampToString(transaction.date)
            if (transaction.amount > 1000) {
                binding.tvAmount.setTextColor(android.graphics.Color.parseColor("#F44336"))
            } else {
                binding.tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
        }
    }
}

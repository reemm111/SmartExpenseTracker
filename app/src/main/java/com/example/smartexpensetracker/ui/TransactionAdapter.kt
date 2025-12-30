package com.example.smartexpensetracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onDelete: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun getItemCount() = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.binding.apply {
            tvCategory.text = transaction.category
            tvNote.text = if (transaction.note.isNotEmpty()) transaction.note else ""
            tvAmount.text = String.format("$%.2f", transaction.amount)
            tvType.text = transaction.type
            
            // Set amount color based on type
            tvAmount.setTextColor(
                if (transaction.type.lowercase() == "income") 
                    holder.itemView.context.getColor(android.R.color.holo_green_dark)
                else 
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
            )
            
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = sdf.format(Date(transaction.date))
            btnDelete.setOnClickListener { onDelete(transaction) }
        }
    }

    fun updateList(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
    
    fun getTransactionAt(position: Int): Transaction = transactions[position]
}

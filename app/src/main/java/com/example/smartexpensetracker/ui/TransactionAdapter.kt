package com.example.smartexpensetracker.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.databinding.ItemTransactionBinding
import com.example.smartexpensetracker.utils.CurrencyConversionHelper
import com.example.smartexpensetracker.utils.CurrencyPreferences
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onDelete: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var selectedCurrency: String = "USD"

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
        val context = holder.itemView.context
        
        holder.binding.apply {
            tvCategory.text = transaction.category
            tvNote.text = if (transaction.note.isNotEmpty()) transaction.note else "No note"
            
            // Set icon based on transaction type
            ivTypeIcon.setImageResource(
                if (transaction.type.lowercase() == "income")
                    com.example.smartexpensetracker.R.drawable.ic_income
                else
                    com.example.smartexpensetracker.R.drawable.ic_expense
            )
            
            // Convert amount to selected currency
            val convertedAmount = CurrencyConversionHelper.convertFromUSD(
                transaction.amount,
                selectedCurrency,
                context
            )
            tvAmount.text = CurrencyConversionHelper.formatAmount(convertedAmount, selectedCurrency)
            
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
            
            // Make the entire item clickable for editing
            root.setOnClickListener {
                val intent = Intent(context, AddTransactionActivity::class.java).apply {
                    putExtra(AddTransactionActivity.EXTRA_TRANSACTION_ID, transaction.id)
                    putExtra(AddTransactionActivity.EXTRA_AMOUNT, transaction.amount)
                    putExtra(AddTransactionActivity.EXTRA_CATEGORY, transaction.category)
                    putExtra(AddTransactionActivity.EXTRA_NOTE, transaction.note)
                    putExtra(AddTransactionActivity.EXTRA_TYPE, transaction.type)
                }
                context.startActivity(intent)
            }
        }
    }

    fun updateList(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
    
    fun updateCurrency(currency: String) {
        selectedCurrency = currency
        notifyDataSetChanged()
    }
    
    fun getTransactionAt(position: Int): Transaction = transactions[position]
}

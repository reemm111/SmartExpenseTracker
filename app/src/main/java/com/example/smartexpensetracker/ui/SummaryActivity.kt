package com.example.smartexpensetracker.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpensetracker.databinding.ActivitySummaryBinding
import com.example.smartexpensetracker.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: CategorySummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeTransactions()
    }

    private fun setupRecyclerView() {
        adapter = CategorySummaryAdapter(emptyList())
        binding.rvCategorySummary.layoutManager = LinearLayoutManager(this)
        binding.rvCategorySummary.adapter = adapter
    }

    private fun observeTransactions() {
        viewModel.allTransactions.observe(this) { transactions ->
            // Filter only expenses
            val expenses = transactions.filter { it.type.lowercase() == "expense" }
            val totalExpense = expenses.sumOf { it.amount }

            // Update total expense display
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            binding.tvTotalExpenseSummary.text = "Total Expenses: ${currencyFormat.format(totalExpense)}"

            // Group by category
            val categoryMap = expenses.groupBy { it.category }
            
            // Calculate summaries
            val summaries = categoryMap.map { (category, transactionList) ->
                val categoryTotal = transactionList.sumOf { it.amount }
                val percentage = if (totalExpense > 0) (categoryTotal / totalExpense) * 100 else 0.0
                
                CategorySummary(
                    categoryName = category,
                    totalAmount = categoryTotal,
                    percentage = percentage,
                    transactionCount = transactionList.size
                )
            }.sortedByDescending { it.totalAmount } // Sort by highest spending

            adapter.updateList(summaries)
        }
    }
}

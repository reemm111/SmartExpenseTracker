package com.example.smartexpensetracker.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.databinding.ActivitySummaryBinding
import com.example.smartexpensetracker.utils.CurrencyConversionHelper
import com.example.smartexpensetracker.utils.CurrencyPreferences
import com.example.smartexpensetracker.utils.PdfExportHelper
import com.example.smartexpensetracker.viewmodel.TransactionViewModel

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: CategorySummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Category Summary"

        setupRecyclerView()
        observeTransactions()
        observeCurrency()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_summary, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_export_summary_pdf -> {
                exportSummaryToPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val currentCurrency = CurrencyPreferences.getSelectedCurrency(this)
        adapter = CategorySummaryAdapter(emptyList(), currentCurrency)
        binding.rvCategorySummary.layoutManager = LinearLayoutManager(this)
        binding.rvCategorySummary.adapter = adapter
    }

    private fun observeTransactions() {
        viewModel.allTransactions.observe(this) { transactions ->
            updateSummary(transactions)
        }
    }
    
    private fun observeCurrency() {
        viewModel.selectedCurrency.observe(this) { currency ->
            // Refresh display when currency changes
            viewModel.allTransactions.value?.let { transactions ->
                updateSummary(transactions)
            }
        }
        
        viewModel.exchangeRatesLoaded.observe(this) { loaded ->
            if (loaded) {
                // Refresh summary when rates are loaded
                viewModel.allTransactions.value?.let { transactions ->
                    updateSummary(transactions)
                }
            }
        }
        
        viewModel.errorMessage.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateSummary(transactions: List<com.example.smartexpensetracker.data.Transaction>) {
        // Filter only expenses
        val expenses = transactions.filter { it.type.lowercase() == "expense" }
        val totalExpense = expenses.sumOf { it.amount }
        
        // Get current currency
        val currency = viewModel.selectedCurrency.value ?: "USD"
        
        // Convert and format total expense
        val convertedTotal = CurrencyConversionHelper.convertFromUSD(totalExpense, currency, this)
        binding.tvTotalExpenseSummary.text = "Total Expenses: ${CurrencyConversionHelper.formatAmount(convertedTotal, currency)}"

        // Group by category
        val categoryMap = expenses.groupBy { it.category }
        
        // Calculate summaries
        val summaries = categoryMap.map { (category, transactionList) ->
            val categoryTotal = transactionList.sumOf { it.amount }
            val percentage = if (totalExpense > 0) (categoryTotal / totalExpense) * 100 else 0.0
            val convertedAmount = CurrencyConversionHelper.convertFromUSD(categoryTotal, currency, this)
            
            CategorySummary(
                categoryName = category,
                totalAmount = convertedAmount,
                percentage = percentage,
                transactionCount = transactionList.size
            )
        }.sortedByDescending { it.totalAmount } // Sort by highest spending

        adapter.updateList(summaries, currency)
    }
    
    private fun exportSummaryToPdf() {
        val transactions = viewModel.allTransactions.value ?: emptyList()
        
        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pdfFile = PdfExportHelper.exportCategorySummaryToPdf(this, transactions)
        
        if (pdfFile != null) {
            Toast.makeText(this, "PDF exported to: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show()
        }
    }
}

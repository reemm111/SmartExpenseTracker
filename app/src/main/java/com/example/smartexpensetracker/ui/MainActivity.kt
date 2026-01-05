package com.example.smartexpensetracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.databinding.ActivityMainBinding
import com.example.smartexpensetracker.utils.BudgetAlertHelper
import com.example.smartexpensetracker.utils.CurrencyConversionHelper
import com.example.smartexpensetracker.utils.CurrencyPreferences
import com.example.smartexpensetracker.utils.PdfExportHelper
import com.example.smartexpensetracker.utils.SwipeToDeleteCallback
import com.example.smartexpensetracker.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    
    private var allTransactions: List<Transaction> = emptyList()
    private var currentFilter: String = "All"
    private var currentSearchQuery: String = ""
    
    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled for budget alerts", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar as the action bar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Smart Expense Tracker"
        
        // Initialize notification channels
        BudgetAlertHelper.createNotificationChannels(this)
        requestNotificationPermission()

        setupRecyclerView()
        setupFAB()
        setupFilterChips()
        observeTransactions()
        observeCurrency()
        
        // Force refresh exchange rates on app start (clear old cache)
        CurrencyPreferences.clearCache(this)
        transactionViewModel.fetchExchangeRates(forceRefresh = true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        // Setup search view
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_summary -> {
                startActivity(Intent(this, SummaryActivity::class.java))
                true
            }
            R.id.action_budget -> {
                startActivity(Intent(this, BudgetSettingsActivity::class.java))
                true
            }
            R.id.action_change_currency -> {
                showCurrencySelectionDialog()
                true
            }
            R.id.action_export_pdf -> {
                exportTransactionsToPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = TransactionAdapter(listOf()) { transaction ->
            transactionViewModel.delete(transaction)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // Setup swipe-to-delete
        setupSwipeToDelete()
    }
    
    private fun setupSwipeToDelete() {
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = adapter.getTransactionAt(position)
                
                // Delete the transaction
                transactionViewModel.delete(transaction)
                
                // Show undo snack bar
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Transaction deleted",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    transactionViewModel.insert(transaction)
                }.show()
            }
        }
        
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupFAB() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun observeTransactions() {
        transactionViewModel.allTransactions.observe(this) { transactions ->
            allTransactions = transactions
            applyFilters()
            updateBalanceSummary(transactions)
            checkBudgetAlerts(transactions)
        }
    }
    
    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipIncome) -> "Income"
                checkedIds.contains(R.id.chipExpense) -> "Expense"
                else -> "All"
            }
            applyFilters()
        }
    }
    
    private fun applyFilters() {
        var filtered = allTransactions
        
        // Apply type filter
        if (currentFilter != "All") {
            filtered = filtered.filter { it.type.equals(currentFilter, ignoreCase = true) }
        }
        
        // Apply search query
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.category.contains(currentSearchQuery, ignoreCase = true) ||
                it.note.contains(currentSearchQuery, ignoreCase = true) ||
                it.amount.toString().contains(currentSearchQuery)
            }
        }
        
        adapter.updateList(filtered)
        
        // Show/hide empty state
        if (filtered.isEmpty()) {
            binding.emptyStateLayout.visibility = android.view.View.VISIBLE
            binding.recyclerView.visibility = android.view.View.GONE
        } else {
            binding.emptyStateLayout.visibility = android.view.View.GONE
            binding.recyclerView.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun observeCurrency() {
        // Observe currency changes and exchange rate loading
        transactionViewModel.selectedCurrency.observe(this) { currency ->
            Log.d("MainActivity", "Currency changed to: $currency")
            
            // Update adapter with new currency
            adapter.updateCurrency(currency)
            
            // Refresh the display when currency changes
            transactionViewModel.allTransactions.value?.let { transactions ->
                updateBalanceSummary(transactions)
            }
        }
        
        transactionViewModel.exchangeRatesLoaded.observe(this) { loaded ->
            Log.d("MainActivity", "Exchange rates loaded: $loaded")
            
            if (loaded) {
                // Refresh display when rates are loaded
                val currency = transactionViewModel.selectedCurrency.value ?: "USD"
                
                transactionViewModel.allTransactions.value?.let { transactions ->
                    updateBalanceSummary(transactions)
                }
                
                // Update adapter with fresh rates
                adapter.updateCurrency(currency)
                
                // Show confirmation
                Toast.makeText(this, "Exchange rates updated", Toast.LENGTH_SHORT).show()
            }
        }
        
        transactionViewModel.errorMessage.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun checkBudgetAlerts(transactions: List<com.example.smartexpensetracker.data.Transaction>) {
        // Only check if budget is set to avoid unnecessary processing
        if (!com.example.smartexpensetracker.utils.BudgetPreferences.hasBudgetSet(this)) {
            return
        }
        
        // 1. Send GLOBAL SYSTEM NOTIFICATIONS (appear in notification tray, even when app is closed)
        BudgetAlertHelper.checkBudgetAndNotify(this, transactions)
        
        // 2. Show IN-APP SNACKBAR for immediate visual feedback (only when app is open)
        val alerts = com.example.smartexpensetracker.utils.BudgetAlertHelper.checkBudgetAlerts(this, transactions)
        
        // Show only the highest priority alert to avoid multiple snack bars
        if (alerts.isNotEmpty()) {
            val criticalAlerts = alerts.filter { it.level == com.example.smartexpensetracker.utils.BudgetAlertHelper.AlertLevel.CRITICAL }
            val warningAlerts = alerts.filter { it.level == com.example.smartexpensetracker.utils.BudgetAlertHelper.AlertLevel.WARNING }
            
            // Show only the first critical alert, or first warning if no critical
            val alertToShow = criticalAlerts.firstOrNull() ?: warningAlerts.firstOrNull()
            
            alertToShow?.let { alert ->
                val backgroundColor = if (alert.level == com.example.smartexpensetracker.utils.BudgetAlertHelper.AlertLevel.CRITICAL) {
                    getColor(android.R.color.holo_red_dark)
                } else {
                    getColor(android.R.color.holo_orange_dark)
                }
                
                // IN-APP ALERT: Snackbar with action button
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    alert.message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setBackgroundTint(backgroundColor)
                .setTextColor(getColor(android.R.color.white))
                .setAction("VIEW BUDGET") {
                    startActivity(Intent(this, BudgetSettingsActivity::class.java))
                }
                .setActionTextColor(getColor(android.R.color.white))
                .show()
            }
        }
    }

    private fun updateBalanceSummary(transactions: List<com.example.smartexpensetracker.data.Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        transactions.forEach { transaction ->
            when (transaction.type.lowercase()) {
                "income" -> totalIncome += transaction.amount
                "expense" -> totalExpense += transaction.amount
            }
        }

        val balance = totalIncome - totalExpense
        
        // Get current currency
        val currency = transactionViewModel.selectedCurrency.value ?: "USD"
        
        // Convert amounts to selected currency
        val convertedIncome = CurrencyConversionHelper.convertFromUSD(totalIncome, currency, this)
        val convertedExpense = CurrencyConversionHelper.convertFromUSD(totalExpense, currency, this)
        val convertedBalance = CurrencyConversionHelper.convertFromUSD(balance, currency, this)

        // Format with currency symbol
        binding.tvTotalIncome.text = CurrencyConversionHelper.formatAmount(convertedIncome, currency)
        binding.tvTotalExpense.text = CurrencyConversionHelper.formatAmount(convertedExpense, currency)
        binding.tvBalance.text = CurrencyConversionHelper.formatAmount(convertedBalance, currency)

        // Change balance color based on positive/negative
        binding.tvBalance.setTextColor(
            if (balance >= 0) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )
    }
    
    private fun showCurrencySelectionDialog() {
        val dialog = CurrencySelectionDialog.newInstance { selectedCurrency ->
            // Save selected currency
            CurrencyPreferences.setSelectedCurrency(this, selectedCurrency)
            
            // Update ViewModel
            transactionViewModel.setSelectedCurrency(selectedCurrency)
            
            // Fetch latest exchange rates
            transactionViewModel.fetchExchangeRates(forceRefresh = true)
            
            // Show feedback
            Toast.makeText(
                this,
                "Currency changed to $selectedCurrency. Fetching latest rates...",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        dialog.show(supportFragmentManager, "CurrencySelectionDialog")
    }
    
    /**
     * Export transactions to PDF and share via email
     */
    private fun exportTransactionsToPdf() {
        val transactions = transactionViewModel.allTransactions.value ?: emptyList()
        
        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show progress indicator
        Toast.makeText(this, "Generating PDF report...", Toast.LENGTH_SHORT).show()
        
        // Export PDF to cache
        val pdfFile = PdfExportHelper.exportTransactionsToPdf(this, transactions)
        
        if (pdfFile != null) {
            // Share via email
            val success = PdfExportHelper.sharePdfViaEmail(
                this,
                pdfFile,
                "My Expense Report - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}",
                "Hi,\n\nPlease find attached my expense report generated by Smart Expense Tracker.\n\nBest regards"
            )
            
            if (success) {
                Log.d("MainActivity", "PDF exported and email chooser opened")
            } else {
                Toast.makeText(this, "No email app found. PDF saved to cache.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "❌ Failed to generate PDF report", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Request notification permission for Android 13+ (TIRAMISU)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation before requesting
                    Toast.makeText(
                        this,
                        "Notifications help you stay on budget with alerts",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission directly
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

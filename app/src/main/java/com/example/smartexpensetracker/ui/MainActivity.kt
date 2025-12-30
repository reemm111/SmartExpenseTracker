package com.example.smartexpensetracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.databinding.ActivityMainBinding
import com.example.smartexpensetracker.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFAB()
        observeTransactions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
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
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(
            object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                0,
                androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean = false
                
                override fun onSwiped(
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    val position = viewHolder.adapterPosition
                    val transaction = adapter.getTransactionAt(position)
                    
                    // Delete the transaction
                    transactionViewModel.delete(transaction)
                    
                    // Show undo snackbar
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "Transaction deleted",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("UNDO") {
                        transactionViewModel.insert(transaction)
                    }.show()
                }
            }
        )
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupFAB() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun observeTransactions() {
        transactionViewModel.allTransactions.observe(this) { transactions ->
            adapter.updateList(transactions)
            updateBalanceSummary(transactions)
            checkBudgetAlerts(transactions)
        }
    }
    
    private fun checkBudgetAlerts(transactions: List<com.example.smartexpensetracker.data.Transaction>) {
        // Only check if budget is set to avoid unnecessary processing
        if (!com.example.smartexpensetracker.utils.BudgetPreferences.hasBudgetSet(this)) {
            return
        }
        
        val alerts = com.example.smartexpensetracker.utils.BudgetAlertHelper.checkBudgetAlerts(this, transactions)
        
        // Show only the highest priority alert to avoid multiple snackbars
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
                
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    alert.message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).setBackgroundTint(backgroundColor)
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
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

        binding.tvTotalIncome.text = currencyFormat.format(totalIncome)
        binding.tvTotalExpense.text = currencyFormat.format(totalExpense)
        binding.tvBalance.text = currencyFormat.format(balance)

        // Change balance color based on positive/negative
        binding.tvBalance.setTextColor(
            if (balance >= 0) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )
    }
}

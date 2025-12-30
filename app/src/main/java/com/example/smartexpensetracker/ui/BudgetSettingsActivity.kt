package com.example.smartexpensetracker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartexpensetracker.databinding.ActivityBudgetSettingsBinding
import com.example.smartexpensetracker.utils.BudgetPreferences

class BudgetSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentBudgets()
        setupSaveButton()
    }

    private fun loadCurrentBudgets() {
        // Load existing budget values
        val overallBudget = BudgetPreferences.getOverallBudget(this)
        if (overallBudget > 0) {
            binding.etOverallBudget.setText(overallBudget.toString())
        }

        val foodBudget = BudgetPreferences.getCategoryBudget(this, "Food")
        if (foodBudget > 0) {
            binding.etFoodBudget.setText(foodBudget.toString())
        }

        val transportationBudget = BudgetPreferences.getCategoryBudget(this, "Transportation")
        if (transportationBudget > 0) {
            binding.etTransportationBudget.setText(transportationBudget.toString())
        }

        val shoppingBudget = BudgetPreferences.getCategoryBudget(this, "Shopping")
        if (shoppingBudget > 0) {
            binding.etShoppingBudget.setText(shoppingBudget.toString())
        }

        val entertainmentBudget = BudgetPreferences.getCategoryBudget(this, "Entertainment")
        if (entertainmentBudget > 0) {
            binding.etEntertainmentBudget.setText(entertainmentBudget.toString())
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveBudget.setOnClickListener {
            saveBudgets()
        }
    }

    private fun saveBudgets() {
        // Save overall budget
        val overallBudgetText = binding.etOverallBudget.text.toString()
        if (overallBudgetText.isNotEmpty()) {
            val overallBudget = overallBudgetText.toDoubleOrNull() ?: 0.0
            BudgetPreferences.setOverallBudget(this, overallBudget)
        }

        // Save category budgets
        saveCategoryBudget("Food", binding.etFoodBudget.text.toString())
        saveCategoryBudget("Transportation", binding.etTransportationBudget.text.toString())
        saveCategoryBudget("Shopping", binding.etShoppingBudget.text.toString())
        saveCategoryBudget("Entertainment", binding.etEntertainmentBudget.text.toString())

        Toast.makeText(this, "Budget settings saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveCategoryBudget(category: String, amountText: String) {
        if (amountText.isNotEmpty()) {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                BudgetPreferences.setCategoryBudget(this, category, amount)
            }
        }
    }
}

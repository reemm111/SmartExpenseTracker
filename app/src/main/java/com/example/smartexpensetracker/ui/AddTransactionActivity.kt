package com.example.smartexpensetracker.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.databinding.ActivityAddTransactionBinding
import com.example.smartexpensetracker.utils.CategorySuggestionHelper
import com.example.smartexpensetracker.viewmodel.TransactionViewModel

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private val viewModel: TransactionViewModel by viewModels()
    private var currentCategories: List<String> = CategorySuggestionHelper.expenseCategories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategorySpinner()
        setupTypeRadioGroup()
        setupAmountWatcher()
        setupButtons()
    }

    private fun setupCategorySpinner() {
        updateCategorySpinner(CategorySuggestionHelper.expenseCategories)
    }

    private fun updateCategorySpinner(categories: List<String>) {
        currentCategories = categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupTypeRadioGroup() {
        binding.rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbExpense -> {
                    updateCategorySpinner(CategorySuggestionHelper.expenseCategories)
                    updateSmartSuggestion()
                }
                R.id.rbIncome -> {
                    updateCategorySpinner(CategorySuggestionHelper.incomeCategories)
                    updateSmartSuggestion()
                }
            }
        }
    }

    private fun setupAmountWatcher() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSmartSuggestion()
            }
        })
    }

    private fun updateSmartSuggestion() {
        val amountText = binding.etAmount.text.toString()
        if (amountText.isEmpty()) {
            binding.tvSuggestion.text = "Enter an amount to see category suggestion"
            return
        }

        val amount = amountText.toDoubleOrNull() ?: 0.0
        val isExpense = binding.rbExpense.isChecked

        val suggestedCategory = if (isExpense) {
            CategorySuggestionHelper.suggestCategoryForExpense(amount)
        } else {
            CategorySuggestionHelper.suggestCategoryForIncome(amount)
        }

        // Auto-select the suggested category in spinner
        val categoryIndex = currentCategories.indexOf(suggestedCategory)
        if (categoryIndex != -1) {
            binding.spinnerCategory.setSelection(categoryIndex)
        }

        binding.tvSuggestion.text = "Suggested: $suggestedCategory (based on amount)"
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveTransaction() {
        val amountText = binding.etAmount.text.toString()
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val category = binding.spinnerCategory.selectedItem?.toString() ?: "Other"
        val note = binding.etNote.text.toString()
        val type = if (binding.rbExpense.isChecked) "Expense" else "Income"

        val transaction = Transaction(
            amount = amount,
            category = category,
            note = note,
            type = type,
            date = System.currentTimeMillis()
        )

        viewModel.insert(transaction)
        Toast.makeText(this, "$type saved successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
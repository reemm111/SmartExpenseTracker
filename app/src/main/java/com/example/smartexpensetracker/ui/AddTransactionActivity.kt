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
    private var editTransactionId: Int = 0
    private var isEditMode = false

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_NOTE = "note"
        const val EXTRA_TYPE = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkEditMode()
        setupCategorySpinner()
        setupTypeRadioGroup()
        setupAmountWatcher()
        setupButtons()
    }
    
    private fun checkEditMode() {
        editTransactionId = intent.getIntExtra(EXTRA_TRANSACTION_ID, 0)
        if (editTransactionId != 0) {
            isEditMode = true
            binding.tvTitle.text = "Edit Transaction"
            binding.btnSave.text = "Update"
            
            val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
            val category = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
            val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
            val type = intent.getStringExtra(EXTRA_TYPE) ?: "Expense"
            
            binding.etAmount.setText(amount.toString())
            binding.etNote.setText(note)
            
            if (type == "Income") {
                binding.rbIncome.isChecked = true
            } else {
                binding.rbExpense.isChecked = true
            }
            
            binding.spinnerCategory.post {
                val categoryIndex = currentCategories.indexOf(category)
                if (categoryIndex != -1) {
                    binding.spinnerCategory.setSelection(categoryIndex)
                }
            }
        } else {
            binding.tvTitle.text = "Add Transaction"
            binding.btnSave.text = "Save"
        }
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
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        binding.btnCancel.isEnabled = false

        val transaction = Transaction(
            id = if (isEditMode) editTransactionId else 0,
            amount = amount,
            category = category,
            note = note,
            type = type,
            date = System.currentTimeMillis()
        )

        if (isEditMode) {
            viewModel.update(transaction)
            Toast.makeText(this, "✅ $type updated successfully!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(transaction)
            Toast.makeText(this, "✅ $type saved successfully!", Toast.LENGTH_SHORT).show()
        }
        
        binding.progressBar.visibility = View.GONE
        binding.btnSave.isEnabled = true
        binding.btnCancel.isEnabled = true
        
        finish()
    }
}
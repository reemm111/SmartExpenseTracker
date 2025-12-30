package com.example.smartexpensetracker.utils

object CategorySuggestionHelper {
    
    // Predefined categories
    val expenseCategories = listOf(
        "Food",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Utilities",
        "Health",
        "Education",
        "Snacks",
        "Rent",
        "Other"
    )
    
    val incomeCategories = listOf(
        "Salary",
        "Freelance",
        "Business",
        "Investment",
        "Gift",
        "Other"
    )
    
    /**
     * Suggests a category based on the transaction amount
     * Smart rules:
     * - 0-5: Snacks
     * - 5-15: Food
     * - 15-30: Transportation
     * - 30-100: Shopping
     * - 100-500: Utilities
     * - 500+: Rent
     */
    fun suggestCategoryForExpense(amount: Double): String {
        return when {
            amount <= 0 -> "Other"
            amount < 5 -> "Snacks"
            amount < 15 -> "Food"
            amount < 30 -> "Transportation"
            amount < 100 -> "Shopping"
            amount < 500 -> "Utilities"
            else -> "Rent"
        }
    }
    
    /**
     * Default suggestion for income
     */
    fun suggestCategoryForIncome(amount: Double): String {
        return when {
            amount < 100 -> "Gift"
            amount < 1000 -> "Freelance"
            else -> "Salary"
        }
    }
}

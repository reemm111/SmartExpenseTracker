package com.example.smartexpensetracker.ui

data class CategorySummary(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double,
    val transactionCount: Int
)

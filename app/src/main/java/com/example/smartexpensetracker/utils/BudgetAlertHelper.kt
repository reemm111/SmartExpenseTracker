package com.example.smartexpensetracker.utils

import android.content.Context
import com.example.smartexpensetracker.data.Transaction

object BudgetAlertHelper {
    
    enum class AlertLevel {
        NONE,
        WARNING,  // 80% of budget
        CRITICAL  // 100% or more of budget
    }
    
    data class BudgetAlert(
        val level: AlertLevel,
        val category: String,
        val spent: Double,
        val budget: Double,
        val percentage: Double,
        val message: String
    )
    
    /**
     * Check if spending exceeds budget thresholds
     */
    fun checkBudgetAlerts(context: Context, transactions: List<Transaction>): List<BudgetAlert> {
        val alerts = mutableListOf<BudgetAlert>()
        
        // Filter expenses only
        val expenses = transactions.filter { it.type.lowercase() == "expense" }
        
        // Check overall budget
        val overallBudget = BudgetPreferences.getOverallBudget(context)
        if (overallBudget > 0) {
            val totalExpense = expenses.sumOf { it.amount }
            val overallPercentage = (totalExpense / overallBudget) * 100
            
            if (overallPercentage >= 100) {
                alerts.add(
                    BudgetAlert(
                        level = AlertLevel.CRITICAL,
                        category = "Overall Budget",
                        spent = totalExpense,
                        budget = overallBudget,
                        percentage = overallPercentage,
                        message = "⚠️ You've exceeded your overall budget by $%.2f!".format(totalExpense - overallBudget)
                    )
                )
            } else if (overallPercentage >= 80) {
                alerts.add(
                    BudgetAlert(
                        level = AlertLevel.WARNING,
                        category = "Overall Budget",
                        spent = totalExpense,
                        budget = overallBudget,
                        percentage = overallPercentage,
                        message = "⚠️ Warning: You've spent %.1f%% of your overall budget".format(overallPercentage)
                    )
                )
            }
        }
        
        // Check category budgets
        val categoryGroups = expenses.groupBy { it.category }
        categoryGroups.forEach { (category, categoryTransactions) ->
            val categoryBudget = BudgetPreferences.getCategoryBudget(context, category)
            if (categoryBudget > 0) {
                val categorySpent = categoryTransactions.sumOf { it.amount }
                val percentage = (categorySpent / categoryBudget) * 100
                
                if (percentage >= 100) {
                    alerts.add(
                        BudgetAlert(
                            level = AlertLevel.CRITICAL,
                            category = category,
                            spent = categorySpent,
                            budget = categoryBudget,
                            percentage = percentage,
                            message = "⚠️ $category budget exceeded by $%.2f!".format(categorySpent - categoryBudget)
                        )
                    )
                } else if (percentage >= 80) {
                    alerts.add(
                        BudgetAlert(
                            level = AlertLevel.WARNING,
                            category = category,
                            spent = categorySpent,
                            budget = categoryBudget,
                            percentage = percentage,
                            message = "⚠️ $category: %.1f%% of budget spent".format(percentage)
                        )
                    )
                }
            }
        }
        
        return alerts.sortedByDescending { it.percentage }
    }
}

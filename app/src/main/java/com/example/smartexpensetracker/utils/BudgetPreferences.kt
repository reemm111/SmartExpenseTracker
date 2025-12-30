package com.example.smartexpensetracker.utils

import android.content.Context
import android.content.SharedPreferences

object BudgetPreferences {
    private const val PREFS_NAME = "budget_preferences"
    private const val KEY_BUDGET_LIMIT = "budget_limit"
    private const val KEY_CATEGORY_PREFIX = "category_budget_"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun setOverallBudget(context: Context, amount: Double) {
        getPreferences(context).edit()
            .putFloat(KEY_BUDGET_LIMIT, amount.toFloat())
            .apply()
    }
    
    fun getOverallBudget(context: Context): Double {
        return getPreferences(context).getFloat(KEY_BUDGET_LIMIT, 0f).toDouble()
    }
    
    fun setCategoryBudget(context: Context, category: String, amount: Double) {
        getPreferences(context).edit()
            .putFloat(KEY_CATEGORY_PREFIX + category, amount.toFloat())
            .apply()
    }
    
    fun getCategoryBudget(context: Context, category: String): Double {
        return getPreferences(context)
            .getFloat(KEY_CATEGORY_PREFIX + category, 0f)
            .toDouble()
    }
    
    fun hasBudgetSet(context: Context): Boolean {
        return getOverallBudget(context) > 0
    }
}

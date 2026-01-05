package com.example.smartexpensetracker.utils

import android.content.Context
import android.util.Log

/**
 * Helper class for currency conversion operations
 */
object CurrencyConversionHelper {
    
    /**
     * Convert amount from USD to target currency
     * @param amount Amount in USD
     * @param targetCurrency Target currency code
     * @param context Context to access cached rates
     * @return Converted amount
     */
    fun convertFromUSD(amount: Double, targetCurrency: String, context: Context): Double {
        if (targetCurrency == "USD") {
            return amount
        }
        
        val rates = CurrencyPreferences.getCachedExchangeRates(context)
        if (rates == null) {
            Log.w("CurrencyConversion", "No cached rates available, returning original amount")
            return amount // Fallback: return original amount
        }
        
        val rate = rates[targetCurrency]
        if (rate == null) {
            Log.w("CurrencyConversion", "No rate found for $targetCurrency, returning original amount")
            return amount // Fallback: return original amount
        }
        
        val converted = amount * rate
        Log.d("CurrencyConversion", "Converted $amount USD to $converted $targetCurrency (rate: $rate)")
        return converted
    }
    
    /**
     * Convert amount to USD from source currency
     * @param amount Amount in source currency
     * @param sourceCurrency Source currency code
     * @param context Context to access cached rates
     * @return Amount in USD
     */
    fun convertToUSD(amount: Double, sourceCurrency: String, context: Context): Double {
        if (sourceCurrency == "USD") {
            return amount
        }
        
        val rates = CurrencyPreferences.getCachedExchangeRates(context)
        if (rates == null) {
            Log.w("CurrencyConversion", "No cached rates available, returning original amount")
            return amount
        }
        
        val rate = rates[sourceCurrency]
        if (rate == null || rate == 0.0) {
            Log.w("CurrencyConversion", "No rate found for $sourceCurrency, returning original amount")
            return amount
        }
        
        val converted = amount / rate
        Log.d("CurrencyConversion", "Converted $amount $sourceCurrency to $converted USD (rate: $rate)")
        return converted
    }
    
    /**
     * Convert between any two currencies
     * @param amount Amount in source currency
     * @param fromCurrency Source currency code
     * @param toCurrency Target currency code
     * @param context Context to access cached rates
     * @return Converted amount
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String, context: Context): Double {
        if (fromCurrency == toCurrency) {
            return amount
        }
        
        // Convert to USD first, then to target currency
        val usdAmount = convertToUSD(amount, fromCurrency, context)
        return convertFromUSD(usdAmount, toCurrency, context)
    }
    
    /**
     * Format amount with currency symbol
     * @param amount Amount to format
     * @param currencyCode Currency code
     * @return Formatted string with currency symbol
     */
    fun formatAmount(amount: Double, currencyCode: String): String {
        val symbol = CurrencyPreferences.getCurrencySymbol(currencyCode)
        return when (currencyCode) {
            "JPY", "KRW" -> {
                // No decimal places for these currencies
                "%s%,.0f".format(symbol, amount)
            }
            else -> {
                "%s%,.2f".format(symbol, amount)
            }
        }
    }
    
    /**
     * Get exchange rate between two currencies
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @param context Context to access cached rates
     * @return Exchange rate, or 1.0 if not available
     */
    fun getExchangeRate(fromCurrency: String, toCurrency: String, context: Context): Double {
        if (fromCurrency == toCurrency) {
            return 1.0
        }
        
        val rates = CurrencyPreferences.getCachedExchangeRates(context) ?: return 1.0
        
        if (fromCurrency == "USD") {
            return rates[toCurrency] ?: 1.0
        }
        
        if (toCurrency == "USD") {
            val fromRate = rates[fromCurrency] ?: return 1.0
            return if (fromRate != 0.0) 1.0 / fromRate else 1.0
        }
        
        // For conversion between two non-USD currencies
        val fromRate = rates[fromCurrency] ?: return 1.0
        val toRate = rates[toCurrency] ?: return 1.0
        
        return if (fromRate != 0.0) toRate / fromRate else 1.0
    }
}

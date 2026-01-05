package com.example.smartexpensetracker.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages currency preferences and exchange rates caching
 */
object CurrencyPreferences {
    private const val PREFS_NAME = "currency_preferences"
    private const val KEY_SELECTED_CURRENCY = "selected_currency"
    private const val KEY_EXCHANGE_RATES = "exchange_rates"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_BASE_CURRENCY = "base_currency"
    
    // Default currency is USD
    private const val DEFAULT_CURRENCY = "USD"
    
    // Cache validity: 1 hour
    private const val CACHE_VALIDITY_MS = 60 * 60 * 1000L
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Set the currently selected currency
     */
    fun setSelectedCurrency(context: Context, currencyCode: String) {
        getPreferences(context).edit()
            .putString(KEY_SELECTED_CURRENCY, currencyCode)
            .apply()
        Log.d("CurrencyPreferences", "Selected currency set to: $currencyCode")
    }
    
    /**
     * Get the currently selected currency
     */
    fun getSelectedCurrency(context: Context): String {
        return getPreferences(context).getString(KEY_SELECTED_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }
    
    /**
     * Save exchange rates to cache
     * Accepts nullable parameters to prevent crashes from invalid API responses
     */
    fun saveExchangeRates(context: Context, rates: Map<String, Double>?, baseCurrency: String? = "USD") {
        if (rates.isNullOrEmpty()) {
            Log.w("CurrencyPreferences", "Cannot save null or empty exchange rates, skipping...")
            return
        }
        
        try {
            val gson = Gson()
            val json = gson.toJson(rates)
            val safeCurrency = baseCurrency ?: "USD"
            
            getPreferences(context).edit()
                .putString(KEY_EXCHANGE_RATES, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .putString(KEY_BASE_CURRENCY, safeCurrency)
                .apply()
            
            Log.d("CurrencyPreferences", "Saved ${rates.size} exchange rates with base $safeCurrency")
        } catch (e: Exception) {
            Log.e("CurrencyPreferences", "Error saving exchange rates: ${e.message}", e)
        }
    }
    
    /**
     * Get cached exchange rates
     */
    fun getCachedExchangeRates(context: Context): Map<String, Double>? {
        val json = getPreferences(context).getString(KEY_EXCHANGE_RATES, null) ?: return null
        
        return try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, Double>>() {}.type
            gson.fromJson<Map<String, Double>>(json, type)
        } catch (e: Exception) {
            Log.e("CurrencyPreferences", "Error parsing cached rates: ${e.message}")
            null
        }
    }
    
    /**
     * Check if cached rates are still valid (less than 24 hours old)
     */
    fun isCacheValid(context: Context): Boolean {
        val lastUpdate = getPreferences(context).getLong(KEY_LAST_UPDATE, 0L)
        val cacheAge = System.currentTimeMillis() - lastUpdate
        val isValid = cacheAge < CACHE_VALIDITY_MS
        
        Log.d("CurrencyPreferences", "Cache age: ${cacheAge / 1000 / 60} minutes, valid: $isValid")
        return isValid
    }
    
    /**
     * Get the base currency used for cached rates
     */
    fun getBaseCurrency(context: Context): String {
        return getPreferences(context).getString(KEY_BASE_CURRENCY, "USD") ?: "USD"
    }
    
    /**
     * Clear all cached exchange rates
     */
    fun clearCache(context: Context) {
        getPreferences(context).edit()
            .remove(KEY_EXCHANGE_RATES)
            .remove(KEY_LAST_UPDATE)
            .remove(KEY_BASE_CURRENCY)
            .apply()
        Log.d("CurrencyPreferences", "Cache cleared")
    }
    
    /**
     * Get supported currencies - 160+ currencies from ExchangeRate-API
     */
    fun getSupportedCurrencies(): List<Pair<String, String>> {
        return listOf(
            // Major Currencies
            "USD" to "US Dollar ($)",
            "EUR" to "Euro (€)",
            "GBP" to "British Pound (£)",
            "JPY" to "Japanese Yen (¥)",
            "CNY" to "Chinese Yuan (¥)",
            "INR" to "Indian Rupee (₹)",
            "CAD" to "Canadian Dollar (C$)",
            "AUD" to "Australian Dollar (A$)",
            "CHF" to "Swiss Franc (CHF)",
            "KRW" to "South Korean Won (₩)",
            
            // Americas
            "ARS" to "Argentine Peso",
            "BRL" to "Brazilian Real (R$)",
            "CLP" to "Chilean Peso",
            "COP" to "Colombian Peso",
            "MXN" to "Mexican Peso",
            "PEN" to "Peruvian Sol",
            "UYU" to "Uruguayan Peso",
            "VEF" to "Venezuelan Bolívar",
            "BOB" to "Bolivian Boliviano",
            "CRC" to "Costa Rican Colón",
            "DOP" to "Dominican Peso",
            "GTQ" to "Guatemalan Quetzal",
            "HNL" to "Honduran Lempira",
            "NIO" to "Nicaraguan Córdoba",
            "PAB" to "Panamanian Balboa",
            "PYG" to "Paraguayan Guaraní",
            "TTD" to "Trinidad & Tobago Dollar",
            "JMD" to "Jamaican Dollar",
            
            // Europe
            "ALL" to "Albanian Lek",
            "AMD" to "Armenian Dram",
            "AZN" to "Azerbaijani Manat",
            "BAM" to "Bosnia-Herzegovina Mark",
            "BGN" to "Bulgarian Lev",
            "BYN" to "Belarusian Ruble",
            "CZK" to "Czech Koruna",
            "DKK" to "Danish Krone",
            "GEL" to "Georgian Lari",
            "HRK" to "Croatian Kuna",
            "HUF" to "Hungarian Forint",
            "ISK" to "Icelandic Króna",
            "MDL" to "Moldovan Leu",
            "MKD" to "Macedonian Denar",
            "NOK" to "Norwegian Krone",
            "PLN" to "Polish Zloty",
            "RON" to "Romanian Leu",
            "RSD" to "Serbian Dinar",
            "RUB" to "Russian Ruble",
            "SEK" to "Swedish Krona",
            "TRY" to "Turkish Lira",
            "UAH" to "Ukrainian Hryvnia",
            
            // Asia
            "AFN" to "Afghan Afghani",
            "BDT" to "Bangladeshi Taka",
            "BND" to "Brunei Dollar",
            "BTC" to "Bitcoin",
            "IDR" to "Indonesian Rupiah",
            "ILS" to "Israeli Shekel",
            "KHR" to "Cambodian Riel",
            "KZT" to "Kazakhstani Tenge",
            "LAK" to "Lao Kip",
            "LKR" to "Sri Lankan Rupee",
            "MMK" to "Myanmar Kyat",
            "MNT" to "Mongolian Tugrik",
            "MYR" to "Malaysian Ringgit",
            "NPR" to "Nepalese Rupee",
            "PHP" to "Philippine Peso",
            "PKR" to "Pakistani Rupee",
            "SGD" to "Singapore Dollar",
            "THB" to "Thai Baht",
            "TWD" to "Taiwan Dollar",
            "UZS" to "Uzbekistani Som",
            "VND" to "Vietnamese Dong",
            "HKD" to "Hong Kong Dollar",
            "MOP" to "Macanese Pataca",
            
            // Middle East
            "AED" to "UAE Dirham",
            "BHD" to "Bahraini Dinar",
            "IQD" to "Iraqi Dinar",
            "IRR" to "Iranian Rial",
            "JOD" to "Jordanian Dinar",
            "KWD" to "Kuwaiti Dinar",
            "LBP" to "Lebanese Pound",
            "OMR" to "Omani Rial",
            "QAR" to "Qatari Riyal",
            "SAR" to "Saudi Riyal",
            "SYP" to "Syrian Pound",
            "YER" to "Yemeni Rial",
            
            // Africa
            "DZD" to "Algerian Dinar",
            "AOA" to "Angolan Kwanza",
            "BWP" to "Botswana Pula",
            "CDF" to "Congolese Franc",
            "EGP" to "Egyptian Pound",
            "ETB" to "Ethiopian Birr",
            "GHS" to "Ghanaian Cedi",
            "GMD" to "Gambian Dalasi",
            "GNF" to "Guinean Franc",
            "KES" to "Kenyan Shilling",
            "MAD" to "Moroccan Dirham",
            "MGA" to "Malagasy Ariary",
            "MUR" to "Mauritian Rupee",
            "MWK" to "Malawian Kwacha",
            "MZN" to "Mozambican Metical",
            "NAD" to "Namibian Dollar",
            "NGN" to "Nigerian Naira",
            "RWF" to "Rwandan Franc",
            "SCR" to "Seychellois Rupee",
            "SLL" to "Sierra Leonean Leone",
            "SOS" to "Somali Shilling",
            "TND" to "Tunisian Dinar",
            "TZS" to "Tanzanian Shilling",
            "UGX" to "Ugandan Shilling",
            "XAF" to "Central African CFA Franc",
            "XOF" to "West African CFA Franc",
            "ZAR" to "South African Rand",
            "ZMW" to "Zambian Kwacha",
            "ZWL" to "Zimbabwean Dollar",
            
            // Oceania
            "FJD" to "Fijian Dollar",
            "NZD" to "New Zealand Dollar",
            "PGK" to "Papua New Guinean Kina",
            "SBD" to "Solomon Islands Dollar",
            "TOP" to "Tongan Paʻanga",
            "VUV" to "Vanuatu Vatu",
            "WST" to "Samoan Tala",
            
            // Other Currencies
            "XCD" to "East Caribbean Dollar",
            "BBD" to "Barbadian Dollar",
            "BMD" to "Bermudian Dollar",
            "BSD" to "Bahamian Dollar",
            "BZD" to "Belize Dollar",
            "CVE" to "Cape Verdean Escudo",
            "DJF" to "Djiboutian Franc",
            "ERN" to "Eritrean Nakfa",
            "FKP" to "Falkland Islands Pound",
            "GIP" to "Gibraltar Pound",
            "HTG" to "Haitian Gourde",
            "KGS" to "Kyrgyzstani Som",
            "KMF" to "Comorian Franc",
            "KYD" to "Cayman Islands Dollar",
            "LRD" to "Liberian Dollar",
            "LSL" to "Lesotho Loti",
            "MRU" to "Mauritanian Ouguiya",
            "MVR" to "Maldivian Rufiyaa",
            "SHP" to "Saint Helena Pound",
            "SRD" to "Surinamese Dollar",
            "STN" to "São Tomé & Príncipe Dobra",
            "SZL" to "Swazi Lilangeni",
            "TJS" to "Tajikistani Somoni",
            "TMT" to "Turkmenistani Manat",
            "XPF" to "CFP Franc"
        )
    }
    
    /**
     * Get currency symbol for a given currency code
     * Comprehensive symbol mapping for 160+ currencies
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode) {
            // Major Currencies
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CNY" -> "¥"
            "INR" -> "₹"
            "CAD" -> "C$"
            "AUD" -> "A$"
            "CHF" -> "CHF"
            "KRW" -> "₩"
            "RUB" -> "₽"
            "BRL" -> "R$"
            "ZAR" -> "R"
            "MXN" -> "$"
            "SEK" -> "kr"
            "NOK" -> "kr"
            "DKK" -> "kr"
            "PLN" -> "zł"
            "TRY" -> "₺"
            "THB" -> "฿"
            "IDR" -> "Rp"
            "MYR" -> "RM"
            "PHP" -> "₱"
            "SGD" -> "S$"
            "HKD" -> "HK$"
            "NZD" -> "NZ$"
            "TWD" -> "NT$"
            "VND" -> "₫"
            "ILS" -> "₪"
            "AED" -> "د.إ"
            "SAR" -> "﷼"
            "QAR" -> "﷼"
            "KWD" -> "د.ك"
            "BHD" -> "د.ب"
            "OMR" -> "﷼"
            "CZK" -> "Kč"
            "HUF" -> "Ft"
            "RON" -> "lei"
            "BGN" -> "лв"
            "HRK" -> "kn"
            "ISK" -> "kr"
            "UAH" -> "₴"
            "PKR" -> "₨"
            "BDT" -> "৳"
            "LKR" -> "₨"
            "NPR" -> "₨"
            "ARS" -> "$"
            "CLP" -> "$"
            "COP" -> "$"
            "PEN" -> "S/"
            "EGP" -> "£"
            "NGN" -> "₦"
            "KES" -> "KSh"
            "GHS" -> "₵"
            "UGX" -> "USh"
            "TZS" -> "TSh"
            "BTC" -> "₿"
            
            // All other currencies default to their code
            else -> currencyCode
        }
    }
}

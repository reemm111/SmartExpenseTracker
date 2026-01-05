package com.example.smartexpensetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.smartexpensetracker.api.RetrofitClient
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.data.TransactionDatabase
import com.example.smartexpensetracker.data.TransactionRepository
import com.example.smartexpensetracker.utils.CurrencyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>

    // LiveData for exchange rates loading status
    private val _exchangeRatesLoaded = MutableLiveData<Boolean>()
    val exchangeRatesLoaded: LiveData<Boolean> = _exchangeRatesLoaded

    // LiveData for selected currency
    private val _selectedCurrency = MutableLiveData<String>()
    val selectedCurrency: LiveData<String> = _selectedCurrency

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        val dao = TransactionDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions

        // Load selected currency
        _selectedCurrency.value = CurrencyPreferences.getSelectedCurrency(application)

        // Check if we have cached rates, if not or expired, fetch new ones
        if (!CurrencyPreferences.isCacheValid(application)) {
            fetchExchangeRates()
        } else {
            _exchangeRatesLoaded.value = true
            Log.d("TransactionViewModel", "Using cached exchange rates")
        }
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>> =
        repository.getTransactionsByCategory(category)

    /**
     * Fetch latest exchange rates from API with comprehensive error handling
     * Prevents crashes from null responses, network failures, or invalid data
     */
    fun fetchExchangeRates(forceRefresh: Boolean = false) {
        // Don't fetch if we already have valid cached rates (unless forced)
        if (!forceRefresh && CurrencyPreferences.isCacheValid(getApplication())) {
            Log.d("TransactionViewModel", "Using valid cached rates, skipping API call")
            _exchangeRatesLoaded.value = true
            return
        }

        viewModelScope.launch {
            try {
                Log.d("TransactionViewModel", "Fetching exchange rates from API...")
                _exchangeRatesLoaded.value = false

                val response = withContext(Dispatchers.IO) {
                    // Try API key endpoint first if key is available
                    if (RetrofitClient.hasApiKey()) {
                        Log.d("TransactionViewModel", "Using API key endpoint")
                        RetrofitClient.exchangeRateApi.getLatestRatesWithKey(RetrofitClient.API_KEY, "USD")
                    } else {
                        Log.d("TransactionViewModel", "Using free endpoint (no API key)")
                        RetrofitClient.exchangeRateApi.getLatestRates("USD")
                    }
                }

                // Comprehensive null-safety checks
                if (!response.isSuccessful) {
                    Log.e("TransactionViewModel", "API request failed: ${response.code()} ${response.message()}")
                    handleFetchFailure("API returned error: ${response.code()}")
                    return@launch
                }

                val responseBody = response.body()
                if (responseBody == null) {
                    Log.e("TransactionViewModel", "Response body is null")
                    handleFetchFailure("Empty response from API")
                    return@launch
                }

                // Validate response data
                if (!responseBody.isValid()) {
                    Log.e("TransactionViewModel", "Invalid response: ${responseBody.error ?: "No rates available"}")
                    handleFetchFailure(responseBody.error ?: "Invalid response data")
                    return@launch
                }

                val rates = responseBody.getRatesMap()
                if (rates.isNullOrEmpty()) {
                    Log.e("TransactionViewModel", "Rates map is null or empty")
                    handleFetchFailure("No exchange rates in response")
                    return@launch
                }

                // Save to cache - now safe with nullable parameters
                CurrencyPreferences.saveExchangeRates(
                    getApplication(),
                    rates,
                    responseBody.getBaseCurrency()
                )

                _exchangeRatesLoaded.value = true
                _errorMessage.value = null
                Log.d("TransactionViewModel", "Exchange rates fetched successfully: ${rates.size} currencies")

            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Exception fetching exchange rates: ${e.message}", e)
                handleFetchFailure("Network error: ${e.message}")
            }
        }
    }

    /**
     * Handle fetch failure with fallback to cached rates
     */
    private fun handleFetchFailure(errorDetail: String) {
        // Try to use cached rates as fallback
        val cachedRates = CurrencyPreferences.getCachedExchangeRates(getApplication())

        if (cachedRates != null && cachedRates.isNotEmpty()) {
            _exchangeRatesLoaded.value = true
            _errorMessage.value = "Using cached rates (offline mode)"
            Log.w("TransactionViewModel", "Using cached rates as fallback: $errorDetail")
        } else {
            _exchangeRatesLoaded.value = false
            _errorMessage.value = "Failed to load exchange rates. Showing amounts in USD."
            Log.e("TransactionViewModel", "No cached rates available: $errorDetail")
        }
    }

    /**
     * Change the selected currency and refresh rates if needed
     */
    fun setSelectedCurrency(currencyCode: String) {
        CurrencyPreferences.setSelectedCurrency(getApplication(), currencyCode)
        _selectedCurrency.value = currencyCode
        Log.d("TransactionViewModel", "Currency changed to: $currencyCode")

        // Refresh rates if cache is invalid
        if (!CurrencyPreferences.isCacheValid(getApplication())) {
            Log.d("TransactionViewModel", "Cache invalid, fetching fresh rates...")
            fetchExchangeRates(forceRefresh = true)
        }
    }

    /**
     * Convert amount from USD to selected currency
     * Safe method with fallback to 1.0 rate if unavailable
     */
    fun convertAmount(amountInUSD: Double): Double {
        val currency = _selectedCurrency.value ?: "USD"
        return convertAmount(amountInUSD, currency)
    }

    /**
     * Convert amount from USD to specific target currency
     * Handles null rates gracefully with 1.0 fallback
     */
    fun convertAmount(amountInUSD: Double, targetCurrency: String): Double {
        // Base currency doesn't need conversion
        if (targetCurrency == "USD") return amountInUSD

        val rates = CurrencyPreferences.getCachedExchangeRates(getApplication())

        // Fallback to 1.0 if rates unavailable
        if (rates == null) {
            Log.w("TransactionViewModel", "No cached rates available for conversion, using 1:1 rate")
            return amountInUSD
        }

        val rate = rates[targetCurrency]
        if (rate == null) {
            Log.w("TransactionViewModel", "No rate found for $targetCurrency, using 1:1 rate")
            return amountInUSD
        }

        return amountInUSD * rate
    }

    /**
     * Get currency symbol for currently selected currency
     */
    fun getCurrencySymbol(): String {
        val currency = _selectedCurrency.value ?: "USD"
        return CurrencyPreferences.getCurrencySymbol(currency)
    }
}

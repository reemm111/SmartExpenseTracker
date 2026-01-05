package com.example.smartexpensetracker.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    
    @GET("v6/{apiKey}/latest/{base}")
    suspend fun getLatestRatesWithKey(
        @Path("apiKey") apiKey: String,
        @Path("base") base: String = "USD"
    ): Response<ExchangeRateResponse>
    
    @GET("v4/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String = "USD"
    ): Response<ExchangeRateResponse>
}


data class ExchangeRateResponse(
    val result: String? = null,           // "success" or "error" (v6 API)
    val base: String? = null,             // Base currency code
    val base_code: String? = null,        // Alternative field name (v6 API)
    val date: String? = null,             // Date of rates
    val time_last_update_unix: Long? = null,  // Timestamp (v6 API)
    val conversion_rates: Map<String, Double>? = null,  // Rates map (v6 API)
    val rates: Map<String, Double>? = null,  // Rates map (v4 API)
    val error: String? = null             // Error message if request fails
) {

    fun getRatesMap(): Map<String, Double>? {
        return conversion_rates ?: rates
    }
    
    /**
     * Get the base currency code regardless of API version
     */
    fun getBaseCurrency(): String {
        return base ?: base_code ?: "USD"
    }
    
    /**
     * Check if response is valid
     */
    fun isValid(): Boolean {
        val ratesMap = getRatesMap()
        return ratesMap != null && ratesMap.isNotEmpty() && 
               (result == null || result == "success")
    }
}

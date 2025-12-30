package com.example.smartexpensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.data.TransactionDatabase
import com.example.smartexpensetracker.data.TransactionRepository
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>

    init {
        val dao = TransactionDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions
    }

    fun insert(transaction: Transaction) = viewModelScope.launch { repository.insert(transaction) }

    fun update(transaction: Transaction) = viewModelScope.launch { repository.update(transaction) }

    fun delete(transaction: Transaction) = viewModelScope.launch { repository.delete(transaction) }

    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>> =
        repository.getTransactionsByCategory(category)
}

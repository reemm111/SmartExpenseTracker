package com.example.smartexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val note: String = "",
    val type: String, // "Income" or "Expense"
    val date: Long = System.currentTimeMillis()
)

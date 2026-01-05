package com.example.smartexpensetracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.databinding.ItemCategorySummaryBinding
import com.example.smartexpensetracker.utils.CurrencyPreferences
import java.text.NumberFormat
import java.util.*

class CategorySummaryAdapter(
    private var summaries: List<CategorySummary>,
    private var currencyCode: String = "USD"
) : RecyclerView.Adapter<CategorySummaryAdapter.CategorySummaryViewHolder>() {

    inner class CategorySummaryViewHolder(val binding: ItemCategorySummaryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySummaryViewHolder {
        val binding = ItemCategorySummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategorySummaryViewHolder(binding)
    }

    override fun getItemCount() = summaries.size

    override fun onBindViewHolder(holder: CategorySummaryViewHolder, position: Int) {
        val summary = summaries[position]
        val symbol = CurrencyPreferences.getCurrencySymbol(currencyCode)

        holder.binding.apply {
            tvCategoryName.text = summary.categoryName
            
            // Format amount based on currency (no decimals for JPY, KRW)
            tvCategoryAmount.text = when (currencyCode) {
                "JPY", "KRW" -> "%s%,.0f".format(symbol, summary.totalAmount)
                else -> "%s%.2f".format(symbol, summary.totalAmount)
            }
            
            tvPercentage.text = String.format("%.1f%%", summary.percentage)
            tvTransactionCount.text = "${summary.transactionCount} transaction${if(summary.transactionCount > 1) "s" else ""}"
            
            progressBar.progress = summary.percentage.toInt()
            
            // Color coding based on spending percentage
            val progressColor = when {
                summary.percentage >= 30 -> Color.parseColor("#F44336") // Red for heavy spending
                summary.percentage >= 15 -> Color.parseColor("#FF9800") // Orange for moderate
                else -> Color.parseColor("#4CAF50") // Green for light spending
            }
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
        }
    }

    fun updateList(newSummaries: List<CategorySummary>, currency: String = "USD") {
        summaries = newSummaries
        currencyCode = currency
        notifyDataSetChanged()
    }
}

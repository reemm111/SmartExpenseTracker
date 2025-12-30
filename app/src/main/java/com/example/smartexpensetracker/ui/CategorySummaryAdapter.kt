package com.example.smartexpensetracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.databinding.ItemCategorySummaryBinding
import java.text.NumberFormat
import java.util.*

class CategorySummaryAdapter(
    private var summaries: List<CategorySummary>
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
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

        holder.binding.apply {
            tvCategoryName.text = summary.categoryName
            tvCategoryAmount.text = currencyFormat.format(summary.totalAmount)
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

    fun updateList(newSummaries: List<CategorySummary>) {
        summaries = newSummaries
        notifyDataSetChanged()
    }
}

package com.example.smartexpensetracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.smartexpensetracker.data.Transaction
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportHelper {
    
    private const val TAG = "PdfExportHelper"
    
    fun exportTransactionsToPdf(
        context: Context,
        transactions: List<Transaction>,
        fileName: String = "expense_report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
    ): File? {
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            val document = Document(PageSize.A4)
            
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()
            
            // Add title
            val titleFont = Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, BaseColor(33, 33, 33))
            val title = Paragraph("Smart Expense Tracker", titleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 10f
            document.add(title)
            
            // Add subtitle
            val subtitleFont = Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, BaseColor.GRAY)
            val subtitle = Paragraph("Transaction Report", subtitleFont)
            subtitle.alignment = Element.ALIGN_CENTER
            subtitle.spacingAfter = 20f
            document.add(subtitle)
            
            // Add date range
            val dateFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.DARK_GRAY)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            val dateRangeText = if (transactions.isNotEmpty()) {
                val earliestDate = transactions.minOfOrNull { it.date } ?: System.currentTimeMillis()
                val latestDate = transactions.maxOfOrNull { it.date } ?: System.currentTimeMillis()
                "Period: ${dateFormat.format(Date(earliestDate))} - ${dateFormat.format(Date(latestDate))}"
            } else {
                "No transactions"
            }
            
            val dateRange = Paragraph(dateRangeText, dateFont)
            dateRange.alignment = Element.ALIGN_CENTER
            dateRange.spacingAfter = 5f
            document.add(dateRange)
            
            // Add generation date
            val genDate = Paragraph("Generated: ${dateFormat.format(Date())}", dateFont)
            genDate.alignment = Element.ALIGN_CENTER
            genDate.spacingAfter = 20f
            document.add(genDate)
            
            // Calculate summary
            val totalIncome = transactions.filter { it.type.equals("Income", ignoreCase = true) }
                .sumOf { it.amount }
            val totalExpense = transactions.filter { it.type.equals("Expense", ignoreCase = true) }
                .sumOf { it.amount }
            val balance = totalIncome - totalExpense
            
            // Add summary section with styled table
            val summaryTable = PdfPTable(2)
            summaryTable.widthPercentage = 60f
            summaryTable.horizontalAlignment = Element.ALIGN_CENTER
            summaryTable.setWidths(floatArrayOf(3f, 2f))
            summaryTable.spacingAfter = 20f
            
            val summaryLabelFont = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, BaseColor.BLACK)
            val summaryValueFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.BLACK)
            
            // Total Income
            summaryTable.addCell(createCell("Total Income:", summaryLabelFont))
            val incomeCell = createCell("$${String.format("%.2f", totalIncome)}", summaryValueFont, Element.ALIGN_RIGHT)
            incomeCell.backgroundColor = BaseColor(200, 255, 200)
            summaryTable.addCell(incomeCell)
            
            // Total Expense
            summaryTable.addCell(createCell("Total Expense:", summaryLabelFont))
            val expenseCell = createCell("$${String.format("%.2f", totalExpense)}", summaryValueFont, Element.ALIGN_RIGHT)
            expenseCell.backgroundColor = BaseColor(255, 200, 200)
            summaryTable.addCell(expenseCell)
            
            // Balance
            summaryTable.addCell(createCell("Balance:", summaryLabelFont))
            val balanceColor = if (balance >= 0) BaseColor(144, 238, 144) else BaseColor(255, 182, 193)
            val balanceCell = createCell("$${String.format("%.2f", balance)}", summaryValueFont, Element.ALIGN_RIGHT)
            balanceCell.backgroundColor = balanceColor
            summaryTable.addCell(balanceCell)
            
            document.add(summaryTable)
            
            // Add transactions header
            val transactionsHeader = Paragraph("Transaction Details", Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, BaseColor.BLACK))
            transactionsHeader.spacingBefore = 10f
            transactionsHeader.spacingAfter = 10f
            document.add(transactionsHeader)
            
            // Create table
            val table = PdfPTable(4) // 4 columns: Date, Category, Note, Amount
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(2f, 2.5f, 3.5f, 2f))
            
            // Add table headers
            val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)
            val headerCells = listOf("Date", "Category", "Note", "Amount")
            
            headerCells.forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = BaseColor(41, 128, 185) // Professional blue
                cell.setPadding(10f)
                cell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(cell)
            }
            
            // Add transaction rows
            val cellFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL, BaseColor.BLACK)
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            transactions.sortedByDescending { it.date }.forEach { transaction ->
                // Date
                table.addCell(createCell(dateFormatter.format(Date(transaction.date)), cellFont))
                
                // Category with type indicator
                val categoryText = "${transaction.category} (${transaction.type})"
                val categoryFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL, 
                    if (transaction.type.equals("Income", ignoreCase = true)) 
                        BaseColor(0, 128, 0) 
                    else 
                        BaseColor(128, 0, 0)
                )
                table.addCell(createCell(categoryText, categoryFont))
                
                // Note
                table.addCell(createCell(transaction.note.ifEmpty { "-" }, cellFont))
                
                // Amount with color coding
                val amountText = if (transaction.type.equals("Income", ignoreCase = true)) {
                    "+$${String.format("%.2f", transaction.amount)}"
                } else {
                    "-$${String.format("%.2f", transaction.amount)}"
                }
                val amountFont = Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD,
                    if (transaction.type.equals("Income", ignoreCase = true)) 
                        BaseColor(0, 128, 0) 
                    else 
                        BaseColor(128, 0, 0)
                )
                table.addCell(createCell(amountText, amountFont, Element.ALIGN_RIGHT))
            }
            
            document.add(table)
            
            // Add footer
            document.add(Paragraph("\n"))
            val footerFont = Font(Font.FontFamily.HELVETICA, 8f, Font.ITALIC, BaseColor.GRAY)
            val footer = Paragraph("Total Transactions: ${transactions.size}", footerFont)
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)
            
            document.close()
            
            Log.d(TAG, "PDF created successfully: ${file.absolutePath}")
            return file
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating PDF: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Export category summary to PDF
     */
    fun exportCategorySummaryToPdf(
        context: Context,
        transactions: List<Transaction>,
        fileName: String = "category_summary_${System.currentTimeMillis()}.pdf"
    ): File? {
        try {
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ExpenseTracker")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            val document = Document(PageSize.A4)
            
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()
            
            // Title
            val titleFont = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BaseColor.BLACK)
            val title = Paragraph("Category Summary Report", titleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 20f
            document.add(title)
            
            // Date
            val dateFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.GRAY)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val dateText = Paragraph("Generated on: ${dateFormat.format(Date())}", dateFont)
            dateText.alignment = Element.ALIGN_CENTER
            dateText.spacingAfter = 15f
            document.add(dateText)
            
            // Group by category
            val expenses = transactions.filter { it.type.equals("Expense", ignoreCase = true) }
            val totalExpense = expenses.sumOf { it.amount }
            
            val categoryGroups = expenses.groupBy { it.category }
            val categorySummary = categoryGroups.map { (category, trans) ->
                val amount = trans.sumOf { it.amount }
                val percentage = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                Triple(category, amount, percentage)
            }.sortedByDescending { it.second }
            
            // Create table
            val table = PdfPTable(3) // Category, Amount, Percentage
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(3f, 2f, 2f))
            
            // Headers
            val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
            val headerCells = listOf("Category", "Amount", "Percentage")
            
            headerCells.forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = BaseColor(52, 152, 219)
                cell.setPadding(10f)
                cell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(cell)
            }
            
            // Data rows
            val cellFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.BLACK)
            
            categorySummary.forEach { (category, amount, percentage) ->
                table.addCell(createCell(category, cellFont))
                table.addCell(createCell("$${String.format("%.2f", amount)}", cellFont, Element.ALIGN_RIGHT))
                table.addCell(createCell("${String.format("%.1f", percentage)}%", cellFont, Element.ALIGN_CENTER))
            }
            
            // Total row
            val totalFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)
            val totalCell1 = createCell("TOTAL", totalFont)
            totalCell1.backgroundColor = BaseColor.LIGHT_GRAY
            table.addCell(totalCell1)
            
            val totalCell2 = createCell("$${String.format("%.2f", totalExpense)}", totalFont, Element.ALIGN_RIGHT)
            totalCell2.backgroundColor = BaseColor.LIGHT_GRAY
            table.addCell(totalCell2)
            
            val totalCell3 = createCell("100%", totalFont, Element.ALIGN_CENTER)
            totalCell3.backgroundColor = BaseColor.LIGHT_GRAY
            table.addCell(totalCell3)
            
            document.add(table)
            document.close()
            
            Log.d(TAG, "Category summary PDF created: ${file.absolutePath}")
            return file
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating category summary PDF: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Helper method to create table cells
     */
    private fun createCell(
        text: String,
        font: Font,
        alignment: Int = Element.ALIGN_LEFT
    ): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.setPadding(8f)
        cell.horizontalAlignment = alignment
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        return cell
    }
    
    /**
     * Share PDF file via email using implicit intent.
     * No permissions required - uses FileProvider for secure file sharing.
     * @param context Application context
     * @param pdfFile The PDF file to share
     * @param emailSubject Subject line for email
     * @param emailBody Body text for email
     * @return True if intent launched successfully, false otherwise
     */
    fun sharePdfViaEmail(
        context: Context,
        pdfFile: File,
        emailSubject: String = "Smart Expense Tracker Report",
        emailBody: String = "Please find attached your transaction report from Smart Expense Tracker.\n\nThis PDF contains a summary of your income, expenses, and detailed transaction list."
    ): Boolean {
        return try {
            // Get URI using FileProvider (secure sharing from cache)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            
            Log.d(TAG, "PDF URI: $uri")
            
            // Create email intent (ACTION_SEND for single attachment)
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Verify there's an app that can handle this intent
            val packageManager = context.packageManager
            if (emailIntent.resolveActivity(packageManager) != null) {
                // Create chooser to let user pick email app
                val chooserIntent = Intent.createChooser(emailIntent, "Send PDF Report")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                context.startActivity(chooserIntent)
                Log.d(TAG, "Email intent launched successfully")
                true
            } else {
                Log.e(TAG, "No email app found to handle intent")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing PDF via email: ${e.message}", e)
            false
        }
    }
    
    /**
     * Export transactions and share via email
     * @param context Application context
     * @param transactions List of transactions to export
     * @return True if successful, false otherwise
     */
    fun exportAndSharePdf(
        context: Context,
        transactions: List<Transaction>
    ): Boolean {
        val pdfFile = exportTransactionsToPdf(context, transactions)
        
        return if (pdfFile != null) {
            sharePdfViaEmail(context, pdfFile)
            true
        } else {
            Log.e(TAG, "Failed to export PDF for sharing")
            false
        }
    }
}

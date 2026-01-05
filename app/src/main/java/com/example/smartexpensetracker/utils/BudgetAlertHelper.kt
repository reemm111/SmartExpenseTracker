package com.example.smartexpensetracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.data.Transaction
import com.example.smartexpensetracker.ui.MainActivity

object BudgetAlertHelper {

    private const val CHANNEL_ID_CRITICAL = "budget_alerts_critical"
    private const val CHANNEL_ID_WARNING = "budget_alerts_warning"
    private const val NOTIFICATION_ID_BASE = 1000

    enum class AlertLevel {
        NONE,
        WARNING,  // 80% of budget
        CRITICAL  // 100% or more of budget
    }

    data class BudgetAlert(
        val level: AlertLevel,
        val category: String,
        val spent: Double,
        val budget: Double,
        val percentage: Double,
        val message: String
    )

    /**
     * Check if spending exceeds budget thresholds
     */
    fun checkBudgetAlerts(context: Context, transactions: List<Transaction>): List<BudgetAlert> {
        val alerts = mutableListOf<BudgetAlert>()

        // Filter expenses only
        val expenses = transactions.filter { it.type.lowercase() == "expense" }

        // Check overall budget
        val overallBudget = BudgetPreferences.getOverallBudget(context)
        if (overallBudget > 0) {
            val totalExpense = expenses.sumOf { it.amount }
            val overallPercentage = (totalExpense / overallBudget) * 100

            if (overallPercentage >= 100) {
                alerts.add(
                    BudgetAlert(
                        level = AlertLevel.CRITICAL,
                        category = "Overall Budget",
                        spent = totalExpense,
                        budget = overallBudget,
                        percentage = overallPercentage,
                        message = "⚠️ You've exceeded your overall budget by $%.2f!".format(
                            totalExpense - overallBudget
                        )
                    )
                )
            } else if (overallPercentage >= 80) {
                alerts.add(
                    BudgetAlert(
                        level = AlertLevel.WARNING,
                        category = "Overall Budget",
                        spent = totalExpense,
                        budget = overallBudget,
                        percentage = overallPercentage,
                        message = "⚠️ Warning: You've spent %.1f%% of your overall budget".format(
                            overallPercentage
                        )
                    )
                )
            }
        }

        // Check category budgets
        val categoryGroups = expenses.groupBy { it.category }
        categoryGroups.forEach { (category, categoryTransactions) ->
            val categoryBudget = BudgetPreferences.getCategoryBudget(context, category)
            if (categoryBudget > 0) {
                val categorySpent = categoryTransactions.sumOf { it.amount }
                val percentage = (categorySpent / categoryBudget) * 100

                if (percentage >= 100) {
                    alerts.add(
                        BudgetAlert(
                            level = AlertLevel.CRITICAL,
                            category = category,
                            spent = categorySpent,
                            budget = categoryBudget,
                            percentage = percentage,
                            message = "⚠️ $category budget exceeded by $%.2f!".format(categorySpent - categoryBudget)
                        )
                    )
                } else if (percentage >= 80) {
                    alerts.add(
                        BudgetAlert(
                            level = AlertLevel.WARNING,
                            category = category,
                            spent = categorySpent,
                            budget = categoryBudget,
                            percentage = percentage,
                            message = "⚠️ $category: %.1f%% of budget spent".format(percentage)
                        )
                    )
                }
            }
        }

        return alerts.sortedByDescending { it.percentage }
    }

    /**
     * Create notification channels for budget alerts (required for Android O+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Critical alerts channel (high priority)
            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Budget Exceeded",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when budget is exceeded"
                enableVibration(true)
                setShowBadge(true)
            }

            // Warning alerts channel (default priority)
            val warningChannel = NotificationChannel(
                CHANNEL_ID_WARNING,
                "Budget Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Warnings when approaching budget limit"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    /**
     * Send system notification for budget alert
     */
    fun sendBudgetNotification(context: Context, alert: BudgetAlert) {
        // Check if notifications should be shown (prevent spam)
        if (!shouldShowNotification(context, alert)) {
            return
        }

        createNotificationChannels(context)

        val channelId = if (alert.level == AlertLevel.CRITICAL)
            CHANNEL_ID_CRITICAL else CHANNEL_ID_WARNING

        val priority = if (alert.level == AlertLevel.CRITICAL)
            NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        // Intent to open MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(
                if (alert.level == AlertLevel.CRITICAL)
                    "Budget Exceeded!"
                else
                    "Budget Warning"
            )
            .setContentText(alert.message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${alert.message}\n\nSpent: $${
                        String.format(
                            "%.2f",
                            alert.spent
                        )
                    } / $${String.format("%.2f", alert.budget)}"
                )
            )
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add color based on alert level
        if (alert.level == AlertLevel.CRITICAL) {
            builder.color = 0xFFFF0000.toInt() // Red
        } else {
            builder.color = 0xFFFFA500.toInt() // Orange
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + alert.category.hashCode()
            notificationManager.notify(notificationId, builder.build())

            // Mark notification as shown
            markNotificationShown(context, alert)
        } catch (e: SecurityException) {
            // Permission not granted - fail silently
        }
    }

    /**
     * Check if notification should be shown (prevent spam)
     * Only show once per hour for same alert
     */
    private fun shouldShowNotification(context: Context, alert: BudgetAlert): Boolean {
        val prefs = context.getSharedPreferences("budget_notifications", Context.MODE_PRIVATE)
        val key = "last_shown_${alert.category}_${alert.level}"
        val lastShown = prefs.getLong(key, 0)
        val now = System.currentTimeMillis()

        // Show notification if more than 1 hour has passed
        return (now - lastShown) > 3600000 // 1 hour
    }

    /**
     * Mark notification as shown to prevent spam
     */
    private fun markNotificationShown(context: Context, alert: BudgetAlert) {
        val prefs = context.getSharedPreferences("budget_notifications", Context.MODE_PRIVATE)
        val key = "last_shown_${alert.category}_${alert.level}"
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    /**
     * Check budget and send notifications if thresholds are exceeded
     */
    fun checkBudgetAndNotify(context: Context, transactions: List<Transaction>) {
        val alerts = checkBudgetAlerts(context, transactions)

        // Send system notifications for critical and warning alerts
        alerts.forEach { alert ->
            if (alert.level == AlertLevel.CRITICAL || alert.level == AlertLevel.WARNING) {
                sendBudgetNotification(context, alert)
            }
        }
    }
}

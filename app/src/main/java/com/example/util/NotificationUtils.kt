package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationUtils {
    const val CHANNEL_BUDGET = "budget_alerts_channel"
    const val CHANNEL_RECURRING = "recurring_alerts_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for when budget limits reach 80%, 90% or are exceeded"
            }

            val recurringChannel = NotificationChannel(
                CHANNEL_RECURRING,
                "Recurring Transactions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for upcoming or processed recurring transactions"
            }

            manager.createNotificationChannel(budgetChannel)
            manager.createNotificationChannel(recurringChannel)
        }
    }

    fun showBudgetWarning(context: Context, categoryOrTotal: String, percentage: Int, currentSpent: String, budgetLimit: String) {
        if (!hasNotificationPermission(context)) return

        val title = when {
            percentage >= 100 -> "⚠️ Budget Exceeded!"
            percentage >= 90 -> "🚨 Budget Alert: 90% Reached"
            else -> "⚡ Budget Warning: 80% Reached"
        }

        val message = "You've spent $currentSpent of your $budgetLimit budget for $categoryOrTotal ($percentage%)."

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(1001 + (categoryOrTotal.hashCode() % 100), builder.build())
    }

    fun showRecurringProcessed(context: Context, title: String, formattedAmount: String) {
        if (!hasNotificationPermission(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_RECURRING)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Recurring Transaction Added")
            .setContentText("$title ($formattedAmount) was automatically recorded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(2000 + (title.hashCode() % 100), builder.build())
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

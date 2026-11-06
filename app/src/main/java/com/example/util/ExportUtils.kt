package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun generateCsv(transactions: List<TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Transaction ID,Title,Type,Amount,Category,Date,Description\n")

        for (tx in transactions) {
            val safeTitle = escapeCsv(tx.title)
            val safeCategory = escapeCsv(tx.category)
            val safeDescription = escapeCsv(tx.description)
            val dateStr = dateFormat.format(Date(tx.date))

            sb.append("${tx.transactionId},$safeTitle,${tx.type},${tx.amount},$safeCategory,$dateStr,$safeDescription\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun saveAndExportCsv(context: Context, transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions available to export", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = generateCsv(transactions)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ExpenseTracker_$timestamp.csv"

        try {
            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    savedUri = uri
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                savedUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            Toast.makeText(context, "CSV downloaded to Downloads: $fileName", Toast.LENGTH_LONG).show()

            // Launch open / share chooser with actual file URI
            if (savedUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, savedUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Records")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Open or Share CSV File")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: save to app external files dir and share via FileProvider
            try {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                FileOutputStream(file).use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                Toast.makeText(context, "CSV downloaded: $fileName", Toast.LENGTH_LONG).show()

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Records")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Open or Share CSV File")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "Failed to download CSV: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

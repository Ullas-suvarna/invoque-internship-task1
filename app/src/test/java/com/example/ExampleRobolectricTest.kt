package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.util.ExportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Expense Tracker", appName)
  }

  @Test
  fun `financial calculation balance is income minus expense`() {
    val income = 50000.0
    val expense = 18500.0
    val balance = income - expense
    assertEquals(31500.0, balance, 0.001)

    val formatted = CurrencyUtils.formatAmount(balance, "₹")
    assertTrue(formatted.contains("₹"))
    assertTrue(formatted.contains("31,500.00"))
  }

  @Test
  fun `csv export generation formats fields correctly`() {
    val tx = TransactionEntity(
      transactionId = "tx-123",
      userId = "user-1",
      title = "Grocery Shopping",
      amount = 1250.50,
      type = TransactionType.EXPENSE.name,
      category = "Food",
      date = 1700000000000L,
      description = "Weekly market trip"
    )

    val csv = ExportUtils.generateCsv(listOf(tx))
    assertTrue(csv.contains("Transaction ID,Title,Type,Amount,Category,Date,Description"))
    assertTrue(csv.contains("tx-123"))
    assertTrue(csv.contains("Grocery Shopping"))
    assertTrue(csv.contains("1250.5"))
    assertTrue(csv.contains("Food"))
  }
}

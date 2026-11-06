package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "Restaurant" -> Icons.Default.Restaurant
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "School" -> Icons.Default.School
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "LocalHospital" -> Icons.Default.LocalHospital
            "Theaters" -> Icons.Default.Theaters
            "Home" -> Icons.Default.Home
            "Receipt" -> Icons.Default.Receipt
            "Flight" -> Icons.Default.Flight
            "Spa" -> Icons.Default.Spa
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "LaptopMac" -> Icons.Default.LaptopMac
            "Store" -> Icons.Default.Store
            "Payments" -> Icons.Default.Payments
            "TrendingUp" -> Icons.Default.TrendingUp
            "MonetizationOn" -> Icons.Default.MonetizationOn
            "MoreHoriz" -> Icons.Default.MoreHoriz
            else -> Icons.Default.Category
        }
    }

    val availableIcons = listOf(
        "Restaurant",
        "ShoppingCart",
        "School",
        "DirectionsCar",
        "LocalHospital",
        "Theaters",
        "Home",
        "Receipt",
        "Flight",
        "Spa",
        "AccountBalanceWallet",
        "LaptopMac",
        "Store",
        "Payments",
        "TrendingUp",
        "MonetizationOn",
        "MoreHoriz"
    )

    val availableColors = listOf(
        0xFFFF7043, // Deep Orange
        0xFFAB47BC, // Purple
        0xFF42A5F5, // Blue
        0xFF26A69A, // Teal
        0xFFEF5350, // Red
        0xFFFFA726, // Amber
        0xFF5C6BC0, // Indigo
        0xFF78909C, // Blue Grey
        0xFF29B6F6, // Light Blue
        0xFFEC407A, // Pink
        0xFF66BB6A, // Green
        0xFF26C6DA, // Cyan
        0xFF8D6E63  // Brown
    )
}

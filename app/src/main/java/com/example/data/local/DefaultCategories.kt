package com.example.data.local

import com.example.data.local.entity.CategoryEntity

object DefaultCategories {
    val expenseCategories = listOf(
        CategoryEntity(
            categoryId = "cat_exp_food",
            userId = "",
            name = "Food",
            type = "EXPENSE",
            iconName = "Restaurant",
            colorHex = 0xFFFF7043,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_shopping",
            userId = "",
            name = "Shopping",
            type = "EXPENSE",
            iconName = "ShoppingCart",
            colorHex = 0xFFAB47BC,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_education",
            userId = "",
            name = "Education",
            type = "EXPENSE",
            iconName = "School",
            colorHex = 0xFF42A5F5,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_transport",
            userId = "",
            name = "Transportation",
            type = "EXPENSE",
            iconName = "DirectionsCar",
            colorHex = 0xFF26A69A,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_healthcare",
            userId = "",
            name = "Healthcare",
            type = "EXPENSE",
            iconName = "LocalHospital",
            colorHex = 0xFFEF5350,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_entertainment",
            userId = "",
            name = "Entertainment",
            type = "EXPENSE",
            iconName = "Theaters",
            colorHex = 0xFFFFA726,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_rent",
            userId = "",
            name = "Rent",
            type = "EXPENSE",
            iconName = "Home",
            colorHex = 0xFF5C6BC0,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_bills",
            userId = "",
            name = "Bills",
            type = "EXPENSE",
            iconName = "Receipt",
            colorHex = 0xFF78909C,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_travel",
            userId = "",
            name = "Travel",
            type = "EXPENSE",
            iconName = "Flight",
            colorHex = 0xFF29B6F6,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_personal_care",
            userId = "",
            name = "Personal Care",
            type = "EXPENSE",
            iconName = "Spa",
            colorHex = 0xFFEC407A,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_exp_other",
            userId = "",
            name = "Other",
            type = "EXPENSE",
            iconName = "MoreHoriz",
            colorHex = 0xFF8D6E63,
            isCustom = false
        )
    )

    val incomeCategories = listOf(
        CategoryEntity(
            categoryId = "cat_inc_salary",
            userId = "",
            name = "Salary",
            type = "INCOME",
            iconName = "AccountBalanceWallet",
            colorHex = 0xFF66BB6A,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_inc_freelancing",
            userId = "",
            name = "Freelancing",
            type = "INCOME",
            iconName = "LaptopMac",
            colorHex = 0xFF26C6DA,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_inc_business",
            userId = "",
            name = "Business",
            type = "INCOME",
            iconName = "Store",
            colorHex = 0xFFFFA000,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_inc_scholarship",
            userId = "",
            name = "Scholarship",
            type = "INCOME",
            iconName = "School",
            colorHex = 0xFF42A5F5,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_inc_allowance",
            userId = "",
            name = "Allowance",
            type = "INCOME",
            iconName = "Payments",
            colorHex = 0xFF8E24AA,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_inc_investments",
            userId = "",
            name = "Investment Returns",
            type = "INCOME",
            iconName = "TrendingUp",
            colorHex = 0xFF43A047,
            isCustom = false
        ),
        CategoryEntity(
            categoryId = "cat_inc_other",
            userId = "",
            name = "Other Income",
            type = "INCOME",
            iconName = "MonetizationOn",
            colorHex = 0xFF00897B,
            isCustom = false
        )
    )

    val allDefaults = expenseCategories + incomeCategories
}

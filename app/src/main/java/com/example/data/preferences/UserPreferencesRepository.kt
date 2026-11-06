package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "expense_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val GUEST_MODE_USER_ID = stringPreferencesKey("guest_mode_user_id")
    }

    val currencySymbol: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "₹"
    }

    val currencyCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY_CODE] ?: "INR"
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(raw)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    val guestUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GUEST_MODE_USER_ID]
    }

    suspend fun setCurrency(symbol: String, code: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
            preferences[PreferencesKeys.CURRENCY_CODE] = code
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setGuestUserId(userId: String?) {
        context.dataStore.edit { preferences ->
            if (userId != null) {
                preferences[PreferencesKeys.GUEST_MODE_USER_ID] = userId
            } else {
                preferences.remove(PreferencesKeys.GUEST_MODE_USER_ID)
            }
        }
    }
}

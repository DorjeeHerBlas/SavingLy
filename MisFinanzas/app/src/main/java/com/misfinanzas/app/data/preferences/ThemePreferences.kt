package com.misfinanzas.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

enum class AppThemeMode(val etiqueta: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
    OLED("OLED")
}

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val oledDarkKey = booleanPreferencesKey("oled_dark")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { preferences ->
        val saved = preferences[themeModeKey]
        saved?.let { value ->
            AppThemeMode.entries.firstOrNull { it.name == value }
        } ?: if (preferences[oledDarkKey] == true) AppThemeMode.OLED else AppThemeMode.SYSTEM
    }

    val oledDark: Flow<Boolean> = themeMode.map { it == AppThemeMode.OLED }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
            preferences[oledDarkKey] = mode == AppThemeMode.OLED
        }
    }

    suspend fun setOledDark(enabled: Boolean) {
        setThemeMode(if (enabled) AppThemeMode.OLED else AppThemeMode.SYSTEM)
    }
}

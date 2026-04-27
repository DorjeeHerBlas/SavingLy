package com.misfinanzas.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiDataStore by preferencesDataStore(name = "api_preferences")

@Singleton
class ApiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apifyTokenKey = stringPreferencesKey("apify_token")

    val apifyToken: Flow<String> = context.apiDataStore.data.map { preferences ->
        preferences[apifyTokenKey].orEmpty()
    }

    suspend fun setApifyToken(token: String) {
        context.apiDataStore.edit { preferences ->
            preferences[apifyTokenKey] = token.trim()
        }
    }
}

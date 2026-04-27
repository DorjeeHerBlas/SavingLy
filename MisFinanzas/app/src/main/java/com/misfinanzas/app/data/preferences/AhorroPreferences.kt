package com.misfinanzas.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ahorroDataStore by preferencesDataStore(name = "ahorro_preferences")

/**
 * Preferencias persistentes de la "Cuenta de Ahorro" virtual.
 *
 * El saldo total se calcula como:
 *   saldoInicial + Σ(ingresos.importe) − Σ(gastos.importe)
 *
 * El usuario puede ajustar [saldoInicial] cuando quiera para que cuadre con su saldo
 * bancario real. A partir de ahí, cualquier ingreso o gasto que registre en la app
 * se irá reflejando automáticamente en el saldo de la cuenta.
 */
@Singleton
class AhorroPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val saldoInicialKey = doublePreferencesKey("saldo_inicial_cuenta")

    val saldoInicial: Flow<Double> = context.ahorroDataStore.data.map { prefs ->
        prefs[saldoInicialKey] ?: 0.0
    }

    suspend fun setSaldoInicial(valor: Double) {
        context.ahorroDataStore.edit { prefs ->
            prefs[saldoInicialKey] = valor.coerceAtLeast(0.0)
        }
    }
}

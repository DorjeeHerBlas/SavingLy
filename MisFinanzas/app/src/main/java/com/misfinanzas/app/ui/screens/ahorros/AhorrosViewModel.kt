package com.misfinanzas.app.ui.screens.ahorros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.data.preferences.AhorroPreferences
import com.misfinanzas.app.domain.model.Aportacion
import com.misfinanzas.app.domain.model.Gasto
import com.misfinanzas.app.domain.model.Ingreso
import com.misfinanzas.app.domain.model.MetaAhorro
import com.misfinanzas.app.domain.repository.AhorroRepository
import com.misfinanzas.app.domain.repository.GastoRepository
import com.misfinanzas.app.domain.repository.IngresoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import javax.inject.Inject

data class AhorrosState(
    val metas: List<MetaAhorro> = emptyList(),
    val mostrarDialogo: Boolean = false,
    val metaEditando: MetaAhorro? = null,
    // Cuenta de ahorro virtual:
    val saldoInicialCuenta: Double = 0.0,
    val totalIngresos: Double = 0.0,
    val totalGastos: Double = 0.0,
    val mostrarDialogoSaldo: Boolean = false,
) {
    /** Saldo dinámico: lo que ya tenías + lo no gastado (ingresos − gastos). */
    val saldoCuentaAhorro: Double
        get() = saldoInicialCuenta + totalIngresos - totalGastos

    /** Lo acumulado por la app desde que se ajustó el saldo inicial. */
    val acumuladoApp: Double
        get() = totalIngresos - totalGastos
}

private data class CuentaAhorroData(
    val saldoInicial: Double,
    val totalIngresos: Double,
    val totalGastos: Double,
)

@HiltViewModel
class AhorrosViewModel @Inject constructor(
    private val repo: AhorroRepository,
    private val gastoRepo: GastoRepository,
    private val ingresoRepo: IngresoRepository,
    private val ahorroPrefs: AhorroPreferences,
) : ViewModel() {

    private val dialogo = MutableStateFlow(false)
    private val editando = MutableStateFlow<MetaAhorro?>(null)
    private val dialogoSaldo = MutableStateFlow(false)

    private val cuentaAhorroFlow: Flow<CuentaAhorroData> = combine(
        ahorroPrefs.saldoInicial,
        gastoRepo.observarTodos().map { gs -> gs.sumOf(Gasto::importe) },
        ingresoRepo.observarTodos().map { ing -> ing.sumOf(Ingreso::importe) },
    ) { saldoIni, totalGastos, totalIngresos ->
        CuentaAhorroData(saldoIni, totalIngresos, totalGastos)
    }

    val estado: StateFlow<AhorrosState> = combine(
        repo.observarMetas(),
        dialogo,
        editando,
        cuentaAhorroFlow,
        dialogoSaldo,
    ) { metas, d, e, cuenta, dSaldo ->
        AhorrosState(
            metas = metas,
            mostrarDialogo = d,
            metaEditando = e,
            saldoInicialCuenta = cuenta.saldoInicial,
            totalIngresos = cuenta.totalIngresos,
            totalGastos = cuenta.totalGastos,
            mostrarDialogoSaldo = dSaldo,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AhorrosState())

    fun abrirNuevo() { editando.value = null; dialogo.value = true }
    fun abrirEditar(m: MetaAhorro) { editando.value = m; dialogo.value = true }
    fun cerrarDialogo() { dialogo.value = false; editando.value = null }

    fun guardar(nombre: String, objetivo: Double, icono: String, fechaLimite: LocalDate?, notas: String?) {
        viewModelScope.launch {
            val edit = editando.value
            val m = edit?.copy(
                nombre = nombre, importeObjetivo = objetivo,
                icono = icono, fechaLimite = fechaLimite, notas = notas
            ) ?: MetaAhorro(
                nombre = nombre, importeObjetivo = objetivo, importeActual = 0.0,
                icono = icono, fechaLimite = fechaLimite, notas = notas
            )
            repo.guardarMeta(m)
            cerrarDialogo()
        }
    }

    fun eliminar(m: MetaAhorro) { viewModelScope.launch { repo.eliminarMeta(m) } }

    fun abrirDialogoSaldo() { dialogoSaldo.value = true }
    fun cerrarDialogoSaldo() { dialogoSaldo.value = false }

    fun setSaldoInicial(valor: Double) {
        viewModelScope.launch {
            ahorroPrefs.setSaldoInicial(valor)
            cerrarDialogoSaldo()
        }
    }
}

data class DetalleMetaState(
    val meta: MetaAhorro? = null,
    val aportaciones: List<Aportacion> = emptyList(),
    val mostrarDialogoAporte: Boolean = false,
    val proyeccionMeses: Int? = null
)

@HiltViewModel
class DetalleMetaViewModel @Inject constructor(
    private val repo: AhorroRepository
) : ViewModel() {

    private val metaIdFlow = MutableStateFlow(0L)
    private val dialogoAporte = MutableStateFlow(false)

    val estado: StateFlow<DetalleMetaState> = metaIdFlow.flatMapLatest { id ->
        if (id == 0L) flowOf(DetalleMetaState()) else combine(
            repo.observarMeta(id),
            repo.observarAportaciones(id),
            dialogoAporte
        ) { m, ap, d ->
            val proyeccion = if (m == null || m.restante <= 0.0 || ap.isEmpty()) {
                null
            } else {
                val primera = ap.minOf { it.fecha }
                val meses = max(1L, ChronoUnit.MONTHS.between(YearMonth.from(primera), YearMonth.now()) + 1)
                val mediaMensual = ap.sumOf { it.importe } / meses
                if (mediaMensual <= 0.0) null else ceil(m.restante / mediaMensual).toInt()
            }
            DetalleMetaState(m, ap, d, proyeccion)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleMetaState())

    fun cargar(id: Long) { metaIdFlow.value = id }
    fun abrirDialogoAporte() { dialogoAporte.value = true }
    fun cerrarDialogoAporte() { dialogoAporte.value = false }

    fun añadirAporte(importe: Double, fecha: LocalDate, nota: String?) {
        val id = metaIdFlow.value
        if (id == 0L) return
        viewModelScope.launch {
            repo.añadirAportacion(Aportacion(metaId = id, importe = importe, fecha = fecha, nota = nota))
            cerrarDialogoAporte()
        }
    }

    fun eliminarAporte(a: Aportacion) {
        viewModelScope.launch { repo.eliminarAportacion(a) }
    }

    fun restaurarAporte(a: Aportacion) {
        viewModelScope.launch { repo.añadirAportacion(a) }
    }
}

package com.misfinanzas.app.ui.screens.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.data.local.dao.TotalCategoria
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.model.FuenteIngreso
import com.misfinanzas.app.domain.model.Gasto
import com.misfinanzas.app.domain.model.GastoRecurrente
import com.misfinanzas.app.domain.model.Ingreso
import com.misfinanzas.app.domain.model.MetodoPago
import com.misfinanzas.app.domain.model.PresupuestoMensual
import com.misfinanzas.app.domain.repository.GastoRecurrenteRepository
import com.misfinanzas.app.domain.repository.GastoRepository
import com.misfinanzas.app.domain.repository.IngresoRepository
import com.misfinanzas.app.domain.repository.PresupuestoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class GastosState(
    val mes: YearMonth = YearMonth.now(),
    val gastos: List<Gasto> = emptyList(),
    val total: Double = 0.0,
    val ingresos: List<Ingreso> = emptyList(),
    val totalIngresos: Double = 0.0,
    val presupuestos: List<PresupuestoMensual> = emptyList(),
    val gastoPorCategoria: List<TotalCategoria> = emptyList(),
    val recurrentes: List<GastoRecurrente> = emptyList(),
    val mensaje: String? = null,
    val mostrarDialogo: Boolean = false,
    val gastoEditando: Gasto? = null
) {
    val balance: Double get() = totalIngresos - total
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GastosViewModel @Inject constructor(
    private val repo: GastoRepository,
    private val ingresosRepo: IngresoRepository,
    private val presupuestosRepo: PresupuestoRepository,
    private val recurrentesRepo: GastoRecurrenteRepository
) : ViewModel() {

    private val mesSel = MutableStateFlow(YearMonth.now())
    private val dialogo = MutableStateFlow(false)
    private val editando = MutableStateFlow<Gasto?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    val estado: StateFlow<GastosState> = mesSel.flatMapLatest { ym ->
        val inicio = ym.atDay(1)
        val fin = ym.atEndOfMonth()
        combine(
            repo.observarRango(inicio, fin),
            repo.observarTotalRango(inicio, fin),
            ingresosRepo.observarRango(inicio, fin),
            ingresosRepo.observarTotalRango(inicio, fin),
            presupuestosRepo.observarMes(ym),
            repo.observarTotalesPorCategoria(inicio, fin),
            recurrentesRepo.observarTodos(),
            mensaje,
            dialogo,
            editando
        ) { values ->
            @Suppress("UNCHECKED_CAST") val lista = values[0] as List<Gasto>
            @Suppress("UNCHECKED_CAST") val ingresos = values[2] as List<Ingreso>
            @Suppress("UNCHECKED_CAST") val presupuestos = values[4] as List<PresupuestoMensual>
            @Suppress("UNCHECKED_CAST") val porCategoria = values[5] as List<TotalCategoria>
            @Suppress("UNCHECKED_CAST") val recurrentes = values[6] as List<GastoRecurrente>
            GastosState(
                mes = ym,
                gastos = lista,
                total = values[1] as Double,
                ingresos = ingresos,
                totalIngresos = values[3] as Double,
                presupuestos = presupuestos,
                gastoPorCategoria = porCategoria,
                recurrentes = recurrentes,
                mensaje = values[7] as String?,
                mostrarDialogo = values[8] as Boolean,
                gastoEditando = values[9] as Gasto?
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GastosState())

    fun cambiarMes(delta: Long) { mesSel.value = mesSel.value.plusMonths(delta) }

    fun abrirNuevo() { editando.value = null; dialogo.value = true }
    fun abrirEditar(g: Gasto) { editando.value = g; dialogo.value = true }
    fun cerrarDialogo() { dialogo.value = false; editando.value = null }

    fun guardar(
        concepto: String, importe: Double, fecha: LocalDate,
        categoria: CategoriaGasto, metodoPago: MetodoPago, nota: String?
    ) {
        viewModelScope.launch {
            val edit = editando.value
            val g = edit?.copy(
                concepto = concepto, importe = importe, fecha = fecha,
                categoria = categoria, metodoPago = metodoPago, nota = nota
            ) ?: Gasto(
                concepto = concepto, importe = importe, fecha = fecha,
                categoria = categoria, metodoPago = metodoPago, nota = nota
            )
            repo.guardar(g)
            cerrarDialogo()
        }
    }

    fun eliminar(g: Gasto) {
        viewModelScope.launch { repo.eliminar(g) }
    }

    fun restaurar(g: Gasto) {
        viewModelScope.launch { repo.guardar(g) }
    }

    fun guardarIngreso(
        concepto: String, importe: Double, fecha: LocalDate,
        fuente: FuenteIngreso, nota: String?
    ) {
        viewModelScope.launch {
            ingresosRepo.guardar(
                Ingreso(concepto = concepto, importe = importe, fecha = fecha, fuente = fuente, nota = nota)
            )
        }
    }

    fun eliminarIngreso(i: Ingreso) {
        viewModelScope.launch { ingresosRepo.eliminar(i) }
    }

    fun restaurarIngreso(i: Ingreso) {
        viewModelScope.launch { ingresosRepo.guardar(i) }
    }

    fun guardarPresupuesto(categoria: CategoriaGasto, limite: Double) {
        val existente = estado.value.presupuestos.firstOrNull { it.categoria == categoria }
        viewModelScope.launch {
            presupuestosRepo.guardar(
                PresupuestoMensual(
                    id = existente?.id ?: 0,
                    mes = mesSel.value,
                    categoria = categoria,
                    limite = limite
                )
            )
        }
    }

    fun eliminarPresupuesto(p: PresupuestoMensual) {
        viewModelScope.launch { presupuestosRepo.eliminar(p) }
    }

    fun guardarRecurrente(
        concepto: String, importe: Double, categoria: CategoriaGasto,
        metodoPago: MetodoPago, diaMes: Int, nota: String?
    ) {
        viewModelScope.launch {
            recurrentesRepo.guardar(
                GastoRecurrente(
                    concepto = concepto,
                    importe = importe,
                    categoria = categoria,
                    metodoPago = metodoPago,
                    diaMes = diaMes.coerceIn(1, 31),
                    nota = nota
                )
            )
        }
    }

    fun toggleRecurrente(g: GastoRecurrente) {
        viewModelScope.launch { recurrentesRepo.guardar(g.copy(activo = !g.activo)) }
    }

    fun eliminarRecurrente(g: GastoRecurrente) {
        viewModelScope.launch { recurrentesRepo.eliminar(g) }
    }

    fun aplicarRecurrentes() {
        viewModelScope.launch {
            val generados = recurrentesRepo.aplicarPendientes(mesSel.value)
            mensaje.value = if (generados == 1) "1 gasto recurrente aplicado" else "$generados gastos recurrentes aplicados"
        }
    }

    fun consumirMensaje() { mensaje.value = null }
}

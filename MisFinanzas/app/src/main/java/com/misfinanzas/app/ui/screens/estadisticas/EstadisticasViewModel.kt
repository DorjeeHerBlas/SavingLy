package com.misfinanzas.app.ui.screens.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.data.local.dao.TotalCategoria
import com.misfinanzas.app.data.local.dao.TotalMetodoPago
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.repository.AhorroRepository
import com.misfinanzas.app.domain.repository.GastoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class EstadisticasState(
    val totalesPorCategoria: List<TotalCategoria> = emptyList(),
    val topCategorias: List<TotalCategoria> = emptyList(),
    val totalesPorMetodo: List<TotalMetodoPago> = emptyList(),
    val totalesPorMes: List<Pair<YearMonth, Double>> = emptyList(),
    val ahorroPorMes: List<Pair<YearMonth, Double>> = emptyList(),
    val totalAnual: Double = 0.0,
    val totalMesActual: Double = 0.0,
    val totalMesAnterior: Double = 0.0,
    val gastoColeccionesAnual: Double = 0.0,
    val gastoEsencialAnual: Double = 0.0
) {
    val diferenciaMes: Double get() = totalMesActual - totalMesAnterior
}

@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    private val gastos: GastoRepository,
    private val ahorros: AhorroRepository
) : ViewModel() {

    val estado: StateFlow<EstadisticasState> = run {
        val hoy = LocalDate.now()
        val mesActual = YearMonth.from(hoy)
        val mesAnterior = mesActual.minusMonths(1)
        val inicioAnyo = hoy.withDayOfYear(1)
        val finAnyo = hoy.withDayOfYear(hoy.lengthOfYear())

        combine(
            gastos.observarTotalesPorCategoria(inicioAnyo, finAnyo) as Flow<Any?>,
            gastos.observarTotalesPorMetodo(inicioAnyo, finAnyo) as Flow<Any?>,
            gastos.observarRango(inicioAnyo, finAnyo) as Flow<Any?>,
            gastos.observarTotalRango(mesActual.atDay(1), mesActual.atEndOfMonth()) as Flow<Any?>,
            gastos.observarTotalRango(mesAnterior.atDay(1), mesAnterior.atEndOfMonth()) as Flow<Any?>,
            ahorros.observarTodasAportaciones() as Flow<Any?>
        ) { values ->
            @Suppress("UNCHECKED_CAST") val porCategoria = values[0] as List<TotalCategoria>
            @Suppress("UNCHECKED_CAST") val porMetodo = values[1] as List<TotalMetodoPago>
            @Suppress("UNCHECKED_CAST") val gastosAnyo = values[2] as List<com.misfinanzas.app.domain.model.Gasto>
            val actual = values[3] as Double
            val anterior = values[4] as Double
            @Suppress("UNCHECKED_CAST") val aportaciones = values[5] as List<com.misfinanzas.app.domain.model.Aportacion>
            val porMes = gastosAnyo.groupBy { YearMonth.from(it.fecha) }
                .mapValues { (_, list) -> list.sumOf { it.importe } }
                .toSortedMap()
                .map { (k, v) -> k to v }

            val ahorroMes = aportaciones.groupBy { YearMonth.from(it.fecha) }
                .mapValues { (_, list) -> list.sumOf { it.importe } }
                .toSortedMap()
                .map { (k, v) -> k to v }

            val gastoColecciones = gastosAnyo
                .filter { it.categoria == CategoriaGasto.COLECCIONES }
                .sumOf { it.importe }
            val gastoEsencial = gastosAnyo
                .filter { it.categoria !in setOf(CategoriaGasto.COLECCIONES, CategoriaGasto.OCIO) }
                .sumOf { it.importe }

            EstadisticasState(
                totalesPorCategoria = porCategoria,
                topCategorias = porCategoria.take(5),
                totalesPorMetodo = porMetodo,
                totalesPorMes = porMes,
                ahorroPorMes = ahorroMes,
                totalAnual = gastosAnyo.sumOf { it.importe },
                totalMesActual = actual,
                totalMesAnterior = anterior,
                gastoColeccionesAnual = gastoColecciones,
                gastoEsencialAnual = gastoEsencial
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadisticasState())
    }
}

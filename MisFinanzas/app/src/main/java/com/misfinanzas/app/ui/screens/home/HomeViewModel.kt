package com.misfinanzas.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.domain.model.Coleccion
import com.misfinanzas.app.domain.model.EstadoItem
import com.misfinanzas.app.domain.model.Gasto
import com.misfinanzas.app.domain.model.Ingreso
import com.misfinanzas.app.domain.model.ItemColeccion
import com.misfinanzas.app.domain.model.MetaAhorro
import com.misfinanzas.app.domain.repository.AhorroRepository
import com.misfinanzas.app.domain.repository.ColeccionRepository
import com.misfinanzas.app.domain.repository.GastoRepository
import com.misfinanzas.app.domain.repository.IngresoRepository
import com.misfinanzas.app.utils.Formato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val gastoMes: Double = 0.0,
    val ingresoMes: Double = 0.0,
    val ahorroTotal: Double = 0.0,
    val metasActivas: Int = 0,
    val totalColecciones: Int = 0,
    val totalItemsTengo: Int = 0,
    val totalItemsQuiero: Int = 0,
    val costeWishlist: Double = 0.0,
    val resultadosBusqueda: List<HomeSearchItem> = emptyList()
) {
    val balanceMes: Double get() = ingresoMes - gastoMes
}

enum class HomeSearchType(val etiqueta: String) {
    GASTO("Gasto"),
    INGRESO("Ingreso"),
    META("Meta"),
    COLECCION("Colección"),
    ITEM("Ítem")
}

data class HomeSearchItem(
    val type: HomeSearchType,
    val title: String,
    val subtitle: String,
    val amount: String? = null,
    val id: Long = 0,
    val parentId: Long = 0
)

private data class TotalesMes(val gastoMes: Double, val ingresoMes: Double)

private data class ResumenColecciones(
    val cols: List<Coleccion>,
    val tengo: List<ItemColeccion>,
    val quiero: List<ItemColeccion>,
    val todosItems: List<ItemColeccion>,
)

private data class HistorialMovimientos(
    val gastos: List<Gasto>,
    val ingresos: List<Ingreso>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gastos: GastoRepository,
    private val ingresos: IngresoRepository,
    private val ahorros: AhorroRepository,
    private val colecciones: ColeccionRepository
) : ViewModel() {

    val estado: StateFlow<HomeState> = run {
        val hoy = LocalDate.now()
        val inicioMes = hoy.withDayOfMonth(1)
        val finMes = hoy.withDayOfMonth(hoy.lengthOfMonth())

        val totalesFlow: Flow<TotalesMes> = combine(
            gastos.observarTotalRango(inicioMes, finMes),
            ingresos.observarTotalRango(inicioMes, finMes),
        ) { gastoMes, ingresoMes -> TotalesMes(gastoMes, ingresoMes) }

        val coleccionesFlow: Flow<ResumenColecciones> = combine(
            colecciones.observarColecciones(),
            colecciones.observarTodosLosItemsPorEstado(EstadoItem.TENGO),
            colecciones.observarTodosLosItemsPorEstado(EstadoItem.QUIERO),
            colecciones.observarTodosLosItems(),
        ) { cols, tengo, quiero, todosItems ->
            ResumenColecciones(cols, tengo, quiero, todosItems)
        }

        val historialFlow: Flow<HistorialMovimientos> = combine(
            gastos.observarTodos(),
            ingresos.observarTodos(),
        ) { gs, ings -> HistorialMovimientos(gs, ings) }

        combine(
            totalesFlow,
            ahorros.observarMetas(),
            coleccionesFlow,
            historialFlow,
        ) { totales, metas, resumen, historial ->
            val coleccionesPorId = resumen.cols.associateBy { it.id }
            val resultados = buildList {
                historial.gastos.forEach { gasto ->
                    add(
                        HomeSearchItem(
                            type = HomeSearchType.GASTO,
                            title = gasto.concepto,
                            subtitle = "${gasto.categoria.emoji} ${gasto.categoria.etiqueta} • ${gasto.metodoPago.emoji} ${gasto.metodoPago.etiqueta} • ${Formato.fecha(gasto.fecha)}",
                            amount = Formato.importe(gasto.importe),
                            id = gasto.id
                        )
                    )
                }
                historial.ingresos.forEach { ingreso ->
                    add(
                        HomeSearchItem(
                            type = HomeSearchType.INGRESO,
                            title = ingreso.concepto,
                            subtitle = "${ingreso.fuente.emoji} ${ingreso.fuente.etiqueta} • ${Formato.fecha(ingreso.fecha)}",
                            amount = Formato.importe(ingreso.importe),
                            id = ingreso.id
                        )
                    )
                }
                metas.forEach { meta ->
                    add(
                        HomeSearchItem(
                            type = HomeSearchType.META,
                            title = meta.nombre,
                            subtitle = "Meta de ahorro • ${(meta.progreso * 100).toInt()}%",
                            amount = Formato.importe(meta.importeActual),
                            id = meta.id
                        )
                    )
                }
                resumen.cols.forEach { coleccion ->
                    add(
                        HomeSearchItem(
                            type = HomeSearchType.COLECCION,
                            title = coleccion.nombre,
                            subtitle = "${coleccion.icono} ${coleccion.tipo.etiqueta}",
                            id = coleccion.id
                        )
                    )
                }
                resumen.todosItems.forEach { item ->
                    val coleccion = coleccionesPorId[item.coleccionId]
                    add(
                        HomeSearchItem(
                            type = HomeSearchType.ITEM,
                            title = item.nombre,
                            subtitle = "${coleccion?.nombre ?: "Colección"} • ${item.estado.etiqueta}",
                            amount = (item.precioPagado ?: item.precio)?.let(Formato::importe),
                            id = item.id,
                            parentId = item.coleccionId
                        )
                    )
                }
            }
            HomeState(
                gastoMes = totales.gastoMes,
                ingresoMes = totales.ingresoMes,
                ahorroTotal = metas.sumOf(MetaAhorro::importeActual),
                metasActivas = metas.count { !it.completada },
                totalColecciones = resumen.cols.size,
                totalItemsTengo = resumen.tengo.sumOf { it.cantidad },
                totalItemsQuiero = resumen.quiero.sumOf { it.cantidad },
                costeWishlist = resumen.quiero.sumOf { (it.precio ?: 0.0) * it.cantidad },
                resultadosBusqueda = resultados
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
    }
}

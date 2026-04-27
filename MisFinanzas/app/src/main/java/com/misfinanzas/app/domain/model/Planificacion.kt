package com.misfinanzas.app.domain.model

import java.time.YearMonth

data class PresupuestoMensual(
    val id: Long = 0,
    val mes: YearMonth,
    val categoria: CategoriaGasto,
    val limite: Double
)

data class GastoRecurrente(
    val id: Long = 0,
    val concepto: String,
    val importe: Double,
    val categoria: CategoriaGasto,
    val metodoPago: MetodoPago = MetodoPago.TARJETA,
    val diaMes: Int = 1,
    val activo: Boolean = true,
    val ultimoGenerado: YearMonth? = null,
    val nota: String? = null
)

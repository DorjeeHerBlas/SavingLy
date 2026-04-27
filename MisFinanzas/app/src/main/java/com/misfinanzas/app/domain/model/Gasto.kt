package com.misfinanzas.app.domain.model

import java.time.LocalDate

data class Gasto(
    val id: Long = 0,
    val concepto: String,
    val importe: Double,
    val fecha: LocalDate,
    val categoria: CategoriaGasto,
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
    val nota: String? = null
)

enum class CategoriaGasto(val etiqueta: String, val emoji: String) {
    ALIMENTACION("Alimentación", "🛒"),
    TRANSPORTE("Transporte", "🚗"),
    OCIO("Ocio", "🎮"),
    HOGAR("Hogar", "🏠"),
    SALUD("Salud", "💊"),
    ROPA("Ropa", "👕"),
    SUSCRIPCIONES("Suscripciones", "📺"),
    COLECCIONES("Colecciones", "📦"),
    OTROS("Otros", "💸")
}

enum class MetodoPago(val etiqueta: String, val emoji: String) {
    EFECTIVO("Efectivo", "💶"),
    TARJETA("Tarjeta", "💳"),
    BIZUM("Bizum", "📲"),
    TRANSFERENCIA("Transferencia", "🏦")
}

data class Ingreso(
    val id: Long = 0,
    val concepto: String,
    val importe: Double,
    val fecha: LocalDate,
    val fuente: FuenteIngreso = FuenteIngreso.OTROS,
    val nota: String? = null
)

enum class FuenteIngreso(val etiqueta: String, val emoji: String) {
    NOMINA("Nómina", "💼"),
    EXTRA("Extra", "✨"),
    VENTA("Venta", "🏷️"),
    REGALO("Regalo", "🎁"),
    OTROS("Otros", "💶")
}

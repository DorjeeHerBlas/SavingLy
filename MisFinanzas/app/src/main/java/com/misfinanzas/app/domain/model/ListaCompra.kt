package com.misfinanzas.app.domain.model

import java.time.LocalDate

/**
 * Categorías predefinidas para la lista de la compra.
 */
enum class CategoriaProducto(val etiqueta: String, val emoji: String) {
    BEBIDAS("Bebidas", "🥤"),
    CARNE_PESCADO("Carne y pescado", "🥩"),
    LACTEOS("Lácteos y huevos", "🥛"),
    PANADERIA("Panadería", "🍞"),
    FRUTAS("Frutas", "🍎"),
    VERDURAS("Verduras", "🥦"),
    DESPENSA("Despensa", "🥫"),
    CONGELADOS("Congelados", "🧊"),
    LIMPIEZA("Limpieza", "🧽"),
    HIGIENE("Higiene", "🧴"),
    HOGAR("Hogar", "🏠"),
    MASCOTAS("Mascotas", "🐾"),
    OTROS("Otros", "📦"),
}

/**
 * Un ítem de la lista de la compra.
 */
data class ItemCompra(
    val id: Long = 0,
    val nombre: String,
    val cantidad: Double = 1.0,
    val unidad: String? = null,        // "kg", "L", "uds"
    val categoria: CategoriaProducto = CategoriaProducto.OTROS,
    val comprado: Boolean = false,
    val precioEstimado: Double? = null,
    val urgente: Boolean = false,
    val notas: String? = null,
    val fechaAdicion: LocalDate = LocalDate.now(),
)

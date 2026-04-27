package com.misfinanzas.app.domain.model

import java.time.LocalDate

/**
 * Una colección agrupa ítems del mismo tipo:
 *  - "Libros de Stephen King"
 *  - "Funko Pops"
 *  - "Master Set Final Fantasy MTG"
 *  - "Videojuegos de PS5"
 */
data class Coleccion(
    val id: Long = 0,
    val nombre: String,
    val tipo: TipoColeccion,
    val descripcion: String? = null,
    val color: Long = 0xFF6750A4,
    val icono: String = "📦",
    val fechaCreacion: LocalDate = LocalDate.now()
)

enum class TipoColeccion(val etiqueta: String, val emoji: String) {
    LIBROS("Libros", "📚"),
    FUNKO_POPS("Funko Pops", "🎎"),
    CARTAS_MTG("Cartas MTG", "🃏"),
    CARTAS_OTRAS("Otras Cartas", "🎴"),
    VIDEOJUEGOS("Videojuegos", "🎮"),
    PELICULAS("Películas", "🎬"),
    MUSICA("Música / Vinilos", "💿"),
    COMICS("Cómics / Manga", "📖"),
    FIGURAS("Figuras", "🗿"),
    LEGO("LEGO", "🧱"),
    OTROS("Otros", "📦")
}

/**
 * Un ítem dentro de una colección.
 * EstadoItem permite separar lo que ya tienes de lo que quieres comprar
 * (wishlist) o lo que está reservado/preordenado.
 */
data class ItemColeccion(
    val id: Long = 0,
    val coleccionId: Long,
    val nombre: String,
    val estado: EstadoItem = EstadoItem.QUIERO,
    val precio: Double? = null,
    val precioPagado: Double? = null,
    val cantidad: Int = 1,
    val fechaAdquisicion: LocalDate? = null,
    val notas: String? = null,
    val rutaImagen: String? = null,
    val urlReferencia: String? = null,
    val condicion: CondicionItem = CondicionItem.NUEVO,
    val esFavorito: Boolean = false,
    val rareza: String? = null,
    val prioridad: PrioridadWishlist = PrioridadWishlist.MEDIA,
    val autor: String? = null,
    val plataforma: String? = null,
    val setColeccion: String? = null,
    val idioma: String? = null,
    val codigoBarras: String? = null
)

enum class EstadoItem(val etiqueta: String, val color: Long) {
    TENGO("Tengo", 0xFF4CAF50),
    QUIERO("Quiero", 0xFFFF9800),
    RESERVADO("Reservado", 0xFF2196F3),
    PRESTADO("Prestado", 0xFF9E9E9E),
    VENDIDO("Vendido", 0xFFE91E63)
}

enum class CondicionItem(val etiqueta: String) {
    NUEVO("Nuevo"),
    COMO_NUEVO("Como nuevo"),
    BUENO("Buen estado"),
    ACEPTABLE("Aceptable"),
    DESGASTADO("Desgastado")
}

enum class PrioridadWishlist(val etiqueta: String, val peso: Int) {
    ALTA("Alta", 0),
    MEDIA("Media", 1),
    BAJA("Baja", 2)
}

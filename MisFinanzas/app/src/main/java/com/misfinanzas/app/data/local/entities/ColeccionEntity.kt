package com.misfinanzas.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.misfinanzas.app.domain.model.*
import java.time.LocalDate

@Entity(tableName = "colecciones")
data class ColeccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipo: TipoColeccion,
    val descripcion: String?,
    val color: Long,
    val icono: String,
    val fechaCreacion: LocalDate
) {
    fun toDomain() = Coleccion(id, nombre, tipo, descripcion, color, icono, fechaCreacion)

    companion object {
        fun fromDomain(c: Coleccion) = ColeccionEntity(
            c.id, c.nombre, c.tipo, c.descripcion, c.color, c.icono, c.fechaCreacion
        )
    }
}

@Entity(
    tableName = "items_coleccion",
    foreignKeys = [ForeignKey(
        entity = ColeccionEntity::class,
        parentColumns = ["id"],
        childColumns = ["coleccionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("coleccionId"), Index("estado")]
)
data class ItemColeccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coleccionId: Long,
    val nombre: String,
    val estado: EstadoItem,
    val precio: Double?,
    val precioPagado: Double?,
    val cantidad: Int,
    val fechaAdquisicion: LocalDate?,
    val notas: String?,
    val rutaImagen: String?,
    val urlReferencia: String?,
    val condicion: CondicionItem,
    val esFavorito: Boolean,
    val rareza: String?,
    val prioridad: PrioridadWishlist,
    val autor: String?,
    val plataforma: String?,
    val setColeccion: String?,
    val idioma: String?,
    val codigoBarras: String?
) {
    fun toDomain() = ItemColeccion(
        id, coleccionId, nombre, estado, precio, precioPagado, cantidad,
        fechaAdquisicion, notas, rutaImagen, urlReferencia, condicion, esFavorito, rareza,
        prioridad, autor, plataforma, setColeccion, idioma, codigoBarras
    )

    companion object {
        fun fromDomain(i: ItemColeccion) = ItemColeccionEntity(
            i.id, i.coleccionId, i.nombre, i.estado, i.precio, i.precioPagado, i.cantidad,
            i.fechaAdquisicion, i.notas, i.rutaImagen, i.urlReferencia,
            i.condicion, i.esFavorito, i.rareza, i.prioridad, i.autor, i.plataforma,
            i.setColeccion, i.idioma, i.codigoBarras
        )
    }
}

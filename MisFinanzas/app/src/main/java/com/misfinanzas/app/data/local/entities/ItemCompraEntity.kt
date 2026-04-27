package com.misfinanzas.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misfinanzas.app.domain.model.CategoriaProducto
import com.misfinanzas.app.domain.model.ItemCompra
import java.time.LocalDate

@Entity(tableName = "items_compra")
data class ItemCompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val cantidad: Double,
    val unidad: String?,
    val categoria: CategoriaProducto,
    val comprado: Boolean,
    val precioEstimado: Double?,
    val urgente: Boolean,
    val notas: String?,
    val fechaAdicion: LocalDate,
) {
    fun toDomain() = ItemCompra(
        id, nombre, cantidad, unidad, categoria, comprado, precioEstimado, urgente, notas, fechaAdicion
    )

    companion object {
        fun fromDomain(i: ItemCompra) = ItemCompraEntity(
            i.id, i.nombre, i.cantidad, i.unidad, i.categoria, i.comprado, i.precioEstimado,
            i.urgente, i.notas, i.fechaAdicion
        )
    }
}

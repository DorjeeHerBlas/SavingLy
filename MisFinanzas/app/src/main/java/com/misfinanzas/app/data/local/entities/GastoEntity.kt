package com.misfinanzas.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.model.FuenteIngreso
import com.misfinanzas.app.domain.model.Gasto
import com.misfinanzas.app.domain.model.Ingreso
import com.misfinanzas.app.domain.model.MetodoPago
import java.time.LocalDate

@Entity(tableName = "gastos")
data class GastoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val importe: Double,
    val fecha: LocalDate,
    val categoria: CategoriaGasto,
    val metodoPago: MetodoPago,
    val nota: String?
) {
    fun toDomain() = Gasto(id, concepto, importe, fecha, categoria, metodoPago, nota)

    companion object {
        fun fromDomain(g: Gasto) = GastoEntity(
            g.id, g.concepto, g.importe, g.fecha, g.categoria, g.metodoPago, g.nota
        )
    }
}

@Entity(tableName = "ingresos")
data class IngresoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val importe: Double,
    val fecha: LocalDate,
    val fuente: FuenteIngreso,
    val nota: String?
) {
    fun toDomain() = Ingreso(id, concepto, importe, fecha, fuente, nota)

    companion object {
        fun fromDomain(i: Ingreso) = IngresoEntity(
            i.id, i.concepto, i.importe, i.fecha, i.fuente, i.nota
        )
    }
}

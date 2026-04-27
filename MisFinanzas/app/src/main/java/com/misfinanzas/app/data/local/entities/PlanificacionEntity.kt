package com.misfinanzas.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.model.GastoRecurrente
import com.misfinanzas.app.domain.model.MetodoPago
import com.misfinanzas.app.domain.model.PresupuestoMensual
import java.time.YearMonth

@Entity(
    tableName = "presupuestos_mensuales",
    indices = [Index(value = ["mes", "categoria"], unique = true)]
)
data class PresupuestoMensualEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mes: YearMonth,
    val categoria: CategoriaGasto,
    val limite: Double
) {
    fun toDomain() = PresupuestoMensual(id, mes, categoria, limite)

    companion object {
        fun fromDomain(p: PresupuestoMensual) =
            PresupuestoMensualEntity(p.id, p.mes, p.categoria, p.limite)
    }
}

@Entity(tableName = "gastos_recurrentes")
data class GastoRecurrenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val importe: Double,
    val categoria: CategoriaGasto,
    val metodoPago: MetodoPago,
    val diaMes: Int,
    val activo: Boolean,
    val ultimoGenerado: YearMonth?,
    val nota: String?
) {
    fun toDomain() = GastoRecurrente(
        id, concepto, importe, categoria, metodoPago, diaMes, activo, ultimoGenerado, nota
    )

    companion object {
        fun fromDomain(g: GastoRecurrente) = GastoRecurrenteEntity(
            g.id, g.concepto, g.importe, g.categoria, g.metodoPago,
            g.diaMes, g.activo, g.ultimoGenerado, g.nota
        )
    }
}

package com.misfinanzas.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.misfinanzas.app.domain.model.Aportacion
import com.misfinanzas.app.domain.model.MetaAhorro
import java.time.LocalDate

@Entity(tableName = "metas_ahorro")
data class MetaAhorroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val importeObjetivo: Double,
    val importeActual: Double,
    val fechaInicio: LocalDate,
    val fechaLimite: LocalDate?,
    val color: Long,
    val icono: String,
    val notas: String?
) {
    fun toDomain() = MetaAhorro(
        id, nombre, importeObjetivo, importeActual, fechaInicio, fechaLimite, color, icono, notas
    )

    companion object {
        fun fromDomain(m: MetaAhorro) = MetaAhorroEntity(
            m.id, m.nombre, m.importeObjetivo, m.importeActual,
            m.fechaInicio, m.fechaLimite, m.color, m.icono, m.notas
        )
    }
}

@Entity(
    tableName = "aportaciones",
    foreignKeys = [ForeignKey(
        entity = MetaAhorroEntity::class,
        parentColumns = ["id"],
        childColumns = ["metaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("metaId")]
)
data class AportacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metaId: Long,
    val importe: Double,
    val fecha: LocalDate,
    val nota: String?
) {
    fun toDomain() = Aportacion(id, metaId, importe, fecha, nota)

    companion object {
        fun fromDomain(a: Aportacion) = AportacionEntity(a.id, a.metaId, a.importe, a.fecha, a.nota)
    }
}

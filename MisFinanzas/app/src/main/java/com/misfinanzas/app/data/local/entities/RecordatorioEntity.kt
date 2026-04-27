package com.misfinanzas.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misfinanzas.app.domain.model.Recordatorio
import com.misfinanzas.app.domain.model.TipoRecordatorio
import java.time.LocalTime

@Entity(tableName = "recordatorios")
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val mensaje: String,
    val tipo: TipoRecordatorio,
    val hora: LocalTime,
    val diasSemana: Set<Int>,
    val activo: Boolean,
    val referenciaId: Long?
) {
    fun toDomain() = Recordatorio(id, titulo, mensaje, tipo, hora, diasSemana, activo, referenciaId)

    companion object {
        fun fromDomain(r: Recordatorio) = RecordatorioEntity(
            r.id, r.titulo, r.mensaje, r.tipo, r.hora, r.diasSemana, r.activo, r.referenciaId
        )
    }
}

package com.misfinanzas.app.domain.model

import java.time.LocalTime

data class Recordatorio(
    val id: Long = 0,
    val titulo: String,
    val mensaje: String,
    val tipo: TipoRecordatorio,
    val hora: LocalTime,
    val diasSemana: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1=Lun, 7=Dom
    val activo: Boolean = true,
    val referenciaId: Long? = null
)

enum class TipoRecordatorio(val etiqueta: String) {
    REGISTRO_GASTO("Registrar gasto diario"),
    REVISION_AHORROS("Revisar ahorros"),
    META_AHORRO("Meta de ahorro"),
    WISHLIST("Item de lista de deseos"),
    PERSONALIZADO("Personalizado")
}

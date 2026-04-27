package com.misfinanzas.app.domain.model

import java.time.LocalDate

data class MetaAhorro(
    val id: Long = 0,
    val nombre: String,
    val importeObjetivo: Double,
    val importeActual: Double = 0.0,
    val fechaInicio: LocalDate = LocalDate.now(),
    val fechaLimite: LocalDate? = null,
    val color: Long = 0xFF4CAF50,
    val icono: String = "🎯",
    val notas: String? = null
) {
    val progreso: Float
        get() = if (importeObjetivo == 0.0) 0f
                else (importeActual / importeObjetivo).coerceIn(0.0, 1.0).toFloat()

    val restante: Double get() = (importeObjetivo - importeActual).coerceAtLeast(0.0)
    val completada: Boolean get() = importeActual >= importeObjetivo
}

data class Aportacion(
    val id: Long = 0,
    val metaId: Long,
    val importe: Double,
    val fecha: LocalDate = LocalDate.now(),
    val nota: String? = null
)
